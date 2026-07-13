package com.walley.app.data.repository

import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.InvestmentEntity
import com.walley.app.data.local.InvestmentTransactionEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.remote.YahooFinanceApi
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class InvestmentRepositoryImpl @Inject constructor(
    private val investmentDao: InvestmentDao,
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
        investmentDao.updateCurrentPrice(investmentId, currentPrice, LocalDate.now())
    }

    override suspend fun deleteInvestment(investmentId: Long) {
        investmentDao.deleteInvestmentWithTransactions(investmentId)
    }

    override suspend fun refreshMarketPrices(investmentIds: Collection<Long>): Map<Long, PriceFetchOutcome> {
        if (!integrationsRepository.observeYahooFinanceEnabled().first()) {
            return investmentIds.associateWith { PriceFetchOutcome.NotFound("Yahoo Finance integration is disabled in Settings") }
        }

        val today = LocalDate.now()
        val results = mutableMapOf<Long, PriceFetchOutcome>()
        investmentIds.forEach { investmentId ->
            val entity = investmentDao.findById(investmentId) ?: return@forEach
            val outcome = fetchOutcomeFor(entity)
            results[investmentId] = outcome
            if (outcome is PriceFetchOutcome.Success) {
                investmentDao.updateCurrentPrice(investmentId, outcome.price, today)
            }
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

        val (quoteCurrency, priceDivisor) = normalizeYahooCurrency(meta.currency)
        val price = if (priceDivisor == BigDecimal.ONE) rawPrice else rawPrice.divide(priceDivisor, 6, RoundingMode.HALF_UP)
        if (quoteCurrency == null || quoteCurrency == entity.currency) {
            return PriceFetchOutcome.Success(price)
        }
        val rate = exchangeRateRepository.observeRates(quoteCurrency).first()?.rates?.get(entity.currency)
            ?: return PriceFetchOutcome.NotFound(
                "Resolved to a $quoteCurrency instrument (expected ${entity.currency.name}), " +
                    "and no exchange rate was available to convert it"
            )
        return PriceFetchOutcome.Success(price.multiply(rate).setScale(6, RoundingMode.HALF_UP))
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
