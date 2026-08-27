package com.walley.app.data.repository

import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.InvestmentEntity
import com.walley.app.data.local.InvestmentPriceHistoryDao
import com.walley.app.data.local.InvestmentPriceHistoryEntity
import com.walley.app.data.local.InvestmentTransactionEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.remote.YahooFinanceApi
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentPricePoint
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class InvestmentRepositoryImpl @Inject constructor(
    private val investmentDao: InvestmentDao,
    private val investmentPriceHistoryDao: InvestmentPriceHistoryDao,
    private val yahooFinanceApi: YahooFinanceApi,
    private val integrationsRepository: IntegrationsRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) : InvestmentRepository {

    override fun observeInvestments(): Flow<List<InvestmentWithTransactions>> =
        combine(investmentDao.observeAll(), investmentDao.observeAllTransactions()) { investments, transactions ->
            investments.map { entity ->
                InvestmentWithTransactions(
                    investment = entity.toDomain(),
                    transactions = transactions.filter { it.investmentId == entity.id }.map { it.toDomain() }
                )
            }
        }

    override fun observeInvestment(investmentId: Long): Flow<InvestmentWithTransactions?> =
        combine(
            investmentDao.observeById(investmentId),
            investmentDao.observeTransactionsForInvestment(investmentId)
        ) { entity, transactions ->
            entity?.let {
                InvestmentWithTransactions(investment = it.toDomain(), transactions = transactions.map { t -> t.toDomain() })
            }
        }

    override suspend fun addInvestment(
        name: String,
        ticker: String,
        category: InvestmentCategory,
        currency: Currency,
        currentPrice: BigDecimal,
        accountId: Long,
        firstPurchaseDate: LocalDate,
        initialQuantity: BigDecimal,
        initialPrice: BigDecimal,
        initialCommission: BigDecimal,
        externalTicker: String?
    ) {
        val investmentId = investmentDao.insert(
            InvestmentEntity(
                name = name,
                ticker = ticker,
                category = category,
                currency = currency,
                currentPrice = currentPrice,
                accountId = accountId,
                externalTicker = externalTicker
            )
        )
        investmentDao.insertTransaction(
            InvestmentTransactionEntity(
                investmentId = investmentId,
                type = InvestmentTransactionType.BUY,
                date = firstPurchaseDate,
                quantity = initialQuantity,
                pricePerUnit = initialPrice,
                commission = initialCommission
            )
        )
    }

    override suspend fun updateInvestmentDetails(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        accountId: Long,
        externalTicker: String?
    ) {
        investmentDao.update(investmentId, name, ticker, category, accountId, externalTicker)
    }

    override suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal) {
        val entity = investmentDao.findById(investmentId) ?: return
        val today = LocalDate.now()
        // BigDecimal.equals() is scale-sensitive (10 != 10.00), so a plain re-save of the same
        // value at a different scale would otherwise be treated as a real change and stomp on the
        // real previousPrice — compareTo is the numeric comparison that avoids that.
        if (entity.currentPrice.compareTo(currentPrice) == 0) {
            investmentDao.touchLastPriceUpdate(investmentId, today)
        } else {
            investmentDao.updateCurrentPrice(investmentId, currentPrice, today)
        }
        recordPriceHistoryPoint(investmentId, currentPrice, today)
    }

    override suspend fun revertToPreviousPrice(investmentId: Long) {
        val today = LocalDate.now()
        investmentDao.revertToPreviousPrice(investmentId, today)
        val reverted = investmentDao.findById(investmentId) ?: return
        recordPriceHistoryPoint(investmentId, reverted.currentPrice, today)
    }

    /** Records today's price so the yearly-growth history stays complete without needing a backfill. */
    private suspend fun recordPriceHistoryPoint(investmentId: Long, price: BigDecimal, date: LocalDate) {
        investmentPriceHistoryDao.insert(InvestmentPriceHistoryEntity(investmentId = investmentId, date = date, closePrice = price))
    }

    override fun observePriceHistory(): Flow<Map<Long, List<InvestmentPricePoint>>> =
        investmentPriceHistoryDao.observeAll().map { entities ->
            entities.groupBy({ it.investmentId }, { it.toDomain() })
        }

    override suspend fun deleteInvestment(investmentId: Long) {
        investmentDao.deleteInvestmentWithTransactions(investmentId)
    }

    override suspend fun refreshMarketPrices(investmentIds: Collection<Long>): Map<Long, PriceFetchOutcome> {
        val entities = investmentIds.mapNotNull { investmentDao.findById(it) }.associateBy { it.id }
        val outcomes = fetchOutcomes(investmentIds, entities)

        val today = LocalDate.now()
        outcomes.forEach { (investmentId, outcome) ->
            if (outcome !is PriceFetchOutcome.Success) return@forEach
            val entity = entities[investmentId] ?: return@forEach
            // Same numeric (scale-independent) comparison as updateCurrentPrice — a refresh that
            // confirms the price hasn't moved still marks it freshly checked, but shouldn't stomp
            // on the real previousPrice with a same-valued "change".
            if (entity.currentPrice.compareTo(outcome.price) == 0) {
                investmentDao.touchLastPriceUpdate(investmentId, today)
            } else {
                investmentDao.updateCurrentPrice(investmentId, outcome.price, today)
            }
            recordPriceHistoryPoint(investmentId, outcome.price, today)
        }
        return outcomes
    }

    override suspend fun fetchMarketPrices(investmentIds: Collection<Long>): Map<Long, PriceFetchOutcome> {
        val entities = investmentIds.mapNotNull { investmentDao.findById(it) }.associateBy { it.id }
        return fetchOutcomes(investmentIds, entities)
    }

    override suspend fun backfillPriceHistory(investmentIds: Collection<Long>): PriceHistoryBackfillResult {
        if (!integrationsRepository.observeYahooFinanceEnabled().first()) {
            return PriceHistoryBackfillResult(succeeded = 0, skipped = investmentIds.size, failed = 0)
        }
        var succeeded = 0
        var skipped = 0
        var failed = 0
        investmentIds.forEachIndexed { index, investmentId ->
            // A short pause between requests to this unofficial, unauthenticated endpoint — see the
            // 429-risk note on YahooFinanceApi — since a full backfill can mean dozens of calls in a row.
            if (index > 0) delay(250)
            val entity = investmentDao.findById(investmentId)
            val symbol = entity?.externalTicker?.takeIf { it.isNotBlank() } ?: entity?.ticker?.takeIf { it.isNotBlank() }
            val firstPurchaseDate = investmentDao.observeTransactionsForInvestment(investmentId).first()
                .filter { it.type == InvestmentTransactionType.BUY }
                .minByOrNull { it.date }?.date
            if (entity == null || symbol == null || firstPurchaseDate == null) {
                skipped++
                return@forEachIndexed
            }
            val points = fetchHistoryFor(entity, symbol, firstPurchaseDate)
            if (points == null) {
                failed++
            } else {
                investmentPriceHistoryDao.insertAll(points)
                succeeded++
            }
        }
        return PriceHistoryBackfillResult(succeeded, skipped, failed)
    }

    /** Null on any network/data error, or if every point ended up unusable (e.g. no FX rate to convert). */
    private suspend fun fetchHistoryFor(
        entity: InvestmentEntity,
        symbol: String,
        firstPurchaseDate: LocalDate
    ): List<InvestmentPriceHistoryEntity>? {
        val response = runCatching {
            yahooFinanceApi.fetchHistory(symbol, firstPurchaseDate.toEpochSecondUtc(), LocalDate.now().toEpochSecondUtc())
        }.getOrElse { return null }
        if (response.chart.error != null) return null
        val result = response.chart.result?.firstOrNull() ?: return null
        val timestamps = result.timestamp ?: return null
        val closes = result.indicators?.quote?.firstOrNull()?.close ?: return null
        val points = timestamps.zip(closes).mapNotNull { (epochSeconds, close) ->
            val rawPrice = close?.let { BigDecimal.valueOf(it) } ?: return@mapNotNull null
            if (rawPrice.signum() <= 0) return@mapNotNull null
            val price = resolvePrice(rawPrice, result.meta.currency, entity.currency) ?: return@mapNotNull null
            InvestmentPriceHistoryEntity(
                investmentId = entity.id,
                date = Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate(),
                closePrice = price
            )
        }
        return points.ifEmpty { null }
    }

    private fun LocalDate.toEpochSecondUtc(): Long = atStartOfDay(ZoneOffset.UTC).toEpochSecond()

    private suspend fun fetchOutcomes(
        investmentIds: Collection<Long>,
        entities: Map<Long, InvestmentEntity>
    ): Map<Long, PriceFetchOutcome> {
        if (!integrationsRepository.observeYahooFinanceEnabled().first()) {
            return investmentIds.associateWith { PriceFetchOutcome.NotFound("Yahoo Finance integration is disabled in Settings") }
        }
        val results = mutableMapOf<Long, PriceFetchOutcome>()
        investmentIds.forEach { investmentId ->
            val entity = entities[investmentId] ?: return@forEach
            results[investmentId] = fetchOutcomeFor(entity)
        }
        return results
    }

    /**
     * Uses [InvestmentEntity.externalTicker] when set — the user-provided Yahoo Finance symbol (e.g.
     * "PZU.WA") — since a plain ticker isn't always resolvable or unambiguous on Yahoo. Falls back to
     * the raw [InvestmentEntity.ticker], which works fine for most US-listed instruments.
     *
     * A quote whose currency doesn't match the investment's own is converted via
     * [exchangeRateRepository] rather than rejected — e.g. a EUR-denominated fund cross-listed on GPW
     * resolves fine even though the investment's own currency is PLN. This is applied regardless of
     * whether [InvestmentEntity.externalTicker] was set, so a bare-ticker lookup that happens to
     * resolve to an unrelated instrument on another exchange will have its price converted and
     * accepted rather than flagged — there's no currency-based signal left to catch that case.
     */
    private suspend fun fetchOutcomeFor(entity: InvestmentEntity): PriceFetchOutcome {
        val symbol = entity.externalTicker?.takeIf { it.isNotBlank() } ?: entity.ticker
        val response = runCatching { yahooFinanceApi.fetchQuote(symbol) }.getOrElse { error ->
            return PriceFetchOutcome.NotFound("Network error: ${error.message ?: error::class.simpleName}")
        }
        response.chart.error?.let { error ->
            return PriceFetchOutcome.NotFound(error.description ?: error.code ?: "Yahoo Finance returned an error")
        }
        val meta = response.chart.result?.firstOrNull()?.meta
            ?: return PriceFetchOutcome.NotFound("No data returned for \"$symbol\"")
        val rawPrice = meta.regularMarketPrice?.let { BigDecimal.valueOf(it) }
            ?: return PriceFetchOutcome.NotFound("No price returned for \"$symbol\"")
        if (rawPrice.signum() <= 0) return PriceFetchOutcome.NotFound("Invalid price returned for \"$symbol\"")

        val price = resolvePrice(rawPrice, meta.currency, entity.currency)
            ?: return PriceFetchOutcome.NotFound(
                "Resolved to a foreign-currency instrument, and no exchange rate was available to convert it to ${entity.currency.name}"
            )
        return PriceFetchOutcome.Success(price)
    }

    /**
     * Converts a Yahoo-quoted [rawPrice] denominated in [currencyCode] into [target], handling the
     * GBp/GBX-in-pence quirk (see [normalizeYahooCurrency]) and any FX conversion needed. Null if
     * [currencyCode] resolves to neither [target] nor a currency with a cached rate to it. Shared by
     * both the live-quote path ([fetchOutcomeFor]) and the historical-backfill path ([fetchHistoryFor]).
     */
    private suspend fun resolvePrice(rawPrice: BigDecimal, currencyCode: String?, target: Currency): BigDecimal? {
        val (quoteCurrency, priceDivisor) = normalizeYahooCurrency(currencyCode)
        val price = if (priceDivisor == BigDecimal.ONE) rawPrice else rawPrice.divide(priceDivisor, 6, RoundingMode.HALF_UP)
        if (quoteCurrency == null || quoteCurrency == target) return price
        val rate = exchangeRateRepository.observeRates(quoteCurrency).first()?.rates?.get(target) ?: return null
        return price.multiply(rate).setScale(6, RoundingMode.HALF_UP)
    }

    /**
     * Yahoo reports LSE-listed prices in pence using the currency code "GBp" (and some other data
     * vendors use "GBX" for the same thing) rather than pounds — a naive case-insensitive match
     * against the "GBP" ISO code would treat pence as pounds and overvalue the position 100x. Both
     * are mapped to GBP with a 100x price divisor; every other code is matched normally (case
     * insensitively) against [Currency], with no divisor.
     */
    private fun normalizeYahooCurrency(code: String?): Pair<Currency?, BigDecimal> {
        if (code == "GBp" || code?.equals("GBX", ignoreCase = true) == true) {
            return Currency.GBP to BigDecimal(100)
        }
        val currency = code?.let { c -> Currency.entries.find { it.name.equals(c, ignoreCase = true) } }
        return currency to BigDecimal.ONE
    }

    override suspend fun addTransaction(
        investmentId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal
    ) {
        investmentDao.insertTransaction(
            InvestmentTransactionEntity(
                investmentId = investmentId,
                type = type,
                date = date,
                quantity = quantity,
                pricePerUnit = pricePerUnit,
                commission = commission
            )
        )
    }

    override suspend fun updateTransaction(
        transactionId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal
    ) {
        investmentDao.updateTransaction(transactionId, type, date, quantity, pricePerUnit, commission)
    }

    override suspend fun deleteTransaction(transactionId: Long) {
        investmentDao.deleteTransaction(transactionId)
    }

    override suspend fun importTransaction(
        accountId: Long,
        ticker: String,
        name: String,
        category: InvestmentCategory,
        currency: Currency,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal
    ) {
        val existing = investmentDao.findByAccountAndTicker(accountId, ticker)
        val investmentId = existing?.id ?: investmentDao.insert(
            InvestmentEntity(
                name = name,
                ticker = ticker,
                category = category,
                currency = currency,
                currentPrice = pricePerUnit,
                accountId = accountId
            )
        )
        investmentDao.insertTransaction(
            InvestmentTransactionEntity(
                investmentId = investmentId,
                type = type,
                date = date,
                quantity = quantity,
                pricePerUnit = pricePerUnit,
                commission = commission
            )
        )
    }
}
