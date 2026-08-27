package com.walley.app.feature.analytics

import com.walley.app.domain.model.AccountOperation
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.InvestmentPricePoint
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.isInternalTransfer
import com.walley.app.domain.model.isInvestmentIncome
import com.walley.app.feature.budget.convertToCurrency
import java.math.BigDecimal
import java.time.LocalDate

data class HoldingYearGrowth(val investmentId: Long, val name: String, val growth: BigDecimal)

data class InvestmentYearGrowthPoint(
    val year: Int,
    val startValue: BigDecimal,
    val endValue: BigDecimal,
    /**
     * Net cash flow into investment accounts this year: deposits minus withdrawals, excluding
     * dividends/interest (see [AccountOperation.isInvestmentIncome]) and transfers between the user's
     * own accounts (see [AccountOperation.isInternalTransfer]) — only genuinely external money in or
     * out counts. A withdrawal-heavy year can make this negative.
     */
    val deposited: BigDecimal,
    /** Gross buys dated this year, across every holding — how much of the cash pool was put into positions, not netted against sells. */
    val invested: BigDecimal,
    /** Dividends and interest received this year, net of withholding tax — real investment return, not a deposit. */
    val investmentIncome: BigDecimal,
    /**
     * Market growth of held positions, net of the estimated capital-gains tax a taxable account would
     * owe if every position were sold at [endValue] — see [afterPotentialTax] — plus [investmentIncome].
     * This is deliberately a mark-to-market figure: a still-open position's paper gain is taxed here
     * exactly as if it had been sold at year-end, even though no real tax is owed until it actually is
     * sold. Also deliberately does NOT subtract [deposited]: cash sitting uninvested in an account was
     * never part of [startValue]/[endValue] in the first place (those only track priced positions), so
     * subtracting it would understate growth by however much cash hasn't been put to work yet.
     */
    val growth: BigDecimal,
    /** True if any holding lacked price coverage for a year boundary and fell back to an estimate. */
    val isEstimated: Boolean,
    /** Market growth per holding, net of potential tax (see [growth]) — does not include [investmentIncome], which isn't attributable to a single holding (e.g. cash-pool interest). */
    val byHolding: List<HoldingYearGrowth>
)

/**
 * Value of this position on [date]: quantity held times the latest known price on/before [date] in
 * [history] (that investment's own price points, in its own currency). [date] on/after [today] always
 * uses [InvestmentWithTransactions.investment]'s live `currentPrice` rather than a history lookup,
 * since that's always exact regardless of how far back price history has been backfilled. Null means
 * quantity was already positive on [date] but no price coverage reaches back that far — the caller
 * should fall back to an estimate rather than silently show zero.
 */
private fun InvestmentWithTransactions.valueAt(date: LocalDate, history: List<InvestmentPricePoint>, today: LocalDate): BigDecimal? {
    val quantity = quantityAvailableOn(date)
    if (quantity.signum() == 0) return BigDecimal.ZERO
    if (!date.isBefore(today)) return quantity * investment.currentPrice
    val price = history.lastOrNull { !it.date.isAfter(date) }?.price ?: return null
    return quantity * price
}

/** Net of this holding's buy/sell events dated in [year] — money that moved into (or out of) this tracked position. */
private fun netTradingCashFlowInYear(data: InvestmentWithTransactions, year: Int): BigDecimal =
    data.transactions
        .filter { it.date.year == year }
        .fold(BigDecimal.ZERO) { sum, t ->
            when (t.type) {
                InvestmentTransactionType.BUY -> sum + t.netAmount
                InvestmentTransactionType.SELL -> sum - t.netAmount
            }
        }

/** Gross buys for this holding dated in [year] — unlike [netTradingCashFlowInYear], not reduced by sells. */
private fun buysInYear(data: InvestmentWithTransactions, year: Int): BigDecimal =
    data.transactions
        .filter { it.type == InvestmentTransactionType.BUY && it.date.year == year }
        .fold(BigDecimal.ZERO) { sum, t -> sum + t.netAmount }

/**
 * [gain] reduced by [taxRate] if it's a gain — never taxes a loss, and matches the simplified,
 * per-holding approach [com.walley.app.domain.model.estimatedTaxForYear] takes for a single account:
 * unlike [com.walley.app.domain.model.estimatePortfolioTax] (used for the app's real, realized-only
 * tax liability estimate), this doesn't net a loss on one holding against a gain on another — each
 * holding is taxed independently. [taxRate] is zero for a tax-free account or an unlinked investment.
 */
private fun afterPotentialTax(gain: BigDecimal, taxRate: BigDecimal): BigDecimal =
    if (gain.signum() > 0 && taxRate.signum() > 0) gain - gain.multiply(taxRate) else gain

/**
 * A held lot's paper gain attributed to the year it was bought (see
 * [InvestmentWithTransactions.unrealizedGainLossByPurchaseYear]) plus realized gains from sells dated
 * in [year] — the pre-price-history approximation, used here only as a fallback for a holding/year
 * that [InvestmentWithTransactions.valueAt] can't value directly.
 */
private fun InvestmentWithTransactions.approximateGainInYear(year: Int): BigDecimal =
    realizedGainLossInYear(year) + (unrealizedGainLossByPurchaseYear()[year] ?: BigDecimal.ZERO)

/**
 * One point per calendar year spanned by [investments]' transactions and [operations], separating
 * three things that used to get conflated into one "contributions" figure: cash you actually
 * [InvestmentYearGrowthPoint.deposited], how much of it got [InvestmentYearGrowthPoint.invested] into
 * positions, and the [InvestmentYearGrowthPoint.growth] those positions (plus dividends/interest)
 * actually produced. A holding/year that [InvestmentWithTransactions.valueAt] can't resolve (no price
 * coverage back that far, or no exchange rate to convert it) falls back to [approximateGainInYear] for
 * just that holding instead of dropping it, and flags the whole year
 * [InvestmentYearGrowthPoint.isEstimated] so the UI can say so. Each holding's gain is also reduced by
 * its account's [accountTaxRateById] rate (see [afterPotentialTax]) before being summed into
 * [InvestmentYearGrowthPoint.growth], so the chart shows what would actually be left after tax if
 * everything were sold at year-end, not the pre-tax paper gain.
 */
fun computeYearlyGrowth(
    investments: List<InvestmentWithTransactions>,
    priceHistoryByInvestment: Map<Long, List<InvestmentPricePoint>>,
    operations: List<AccountOperation>,
    accountCurrencyById: Map<Long, Currency>,
    accountTaxRateById: Map<Long, BigDecimal>,
    base: Currency,
    rates: ExchangeRates?,
    today: LocalDate = LocalDate.now()
): List<InvestmentYearGrowthPoint> {
    val transactionYears = investments.flatMap { data -> data.transactions.map { it.date.year } }
    val operationYears = operations.map { it.date.year }
    val years = transactionYears + operationYears
    if (years.isEmpty()) return emptyList()
    val range = years.min()..maxOf(years.max(), today.year)

    return range.map { year ->
        val yearEnd = minOf(LocalDate.of(year, 12, 31), today)
        val previousYearEnd = LocalDate.of(year, 1, 1).minusDays(1)

        var startValue = BigDecimal.ZERO
        var endValue = BigDecimal.ZERO
        var invested = BigDecimal.ZERO
        var estimated = false
        val byHolding = mutableListOf<HoldingYearGrowth>()

        // A holding not yet bought, or already sold out entirely with nothing happening this year,
        // contributes zero either way — skipping it doesn't change the totals, but it also shouldn't
        // clutter the per-holding breakdown for a year before it existed or after it was closed out.
        val existingHoldings = investments.filter { data ->
            data.quantityAvailableOn(previousYearEnd).signum() > 0 ||
                data.quantityAvailableOn(yearEnd).signum() > 0 ||
                data.transactions.any { it.date.year == year }
        }

        for (data in existingHoldings) {
            invested += convertToCurrency(buysInYear(data, year), data.investment.currency, base, rates) ?: BigDecimal.ZERO
            val taxRate = data.investment.accountId?.let { accountTaxRateById[it] } ?: BigDecimal.ZERO

            val history = priceHistoryByInvestment[data.investment.id].orEmpty()
            val rawStart = data.valueAt(previousYearEnd, history, today)
            val rawEnd = data.valueAt(yearEnd, history, today)
            val convertedStart = rawStart?.let { convertToCurrency(it, data.investment.currency, base, rates) }
            val convertedEnd = rawEnd?.let { convertToCurrency(it, data.investment.currency, base, rates) }

            if (convertedStart == null || convertedEnd == null) {
                estimated = true
                val approxGain = convertToCurrency(data.approximateGainInYear(year), data.investment.currency, base, rates)
                    ?: BigDecimal.ZERO
                byHolding += HoldingYearGrowth(data.investment.id, data.investment.name, afterPotentialTax(approxGain, taxRate))
                continue
            }

            val netForHolding = convertToCurrency(netTradingCashFlowInYear(data, year), data.investment.currency, base, rates)
                ?: BigDecimal.ZERO
            startValue += convertedStart
            endValue += convertedEnd
            val rawGrowth = convertedEnd - convertedStart - netForHolding
            byHolding += HoldingYearGrowth(data.investment.id, data.investment.name, afterPotentialTax(rawGrowth, taxRate))
        }

        val operationsThisYear = operations.filter { it.date.year == year }
        fun sumOperations(matching: (AccountOperation) -> Boolean) = operationsThisYear
            .filter(matching)
            .fold(BigDecimal.ZERO) { acc, operation ->
                val currency = accountCurrencyById[operation.accountId] ?: base
                acc + (convertToCurrency(operation.amount, currency, base, rates) ?: BigDecimal.ZERO)
            }

        val deposited = sumOperations { !it.isInvestmentIncome() && !it.isInternalTransfer() }
        val investmentIncome = sumOperations { it.isInvestmentIncome() }

        InvestmentYearGrowthPoint(
            year = year,
            startValue = startValue,
            endValue = endValue,
            deposited = deposited,
            invested = invested,
            investmentIncome = investmentIncome,
            growth = byHolding.fold(BigDecimal.ZERO) { acc, holding -> acc + holding.growth } + investmentIncome,
            isEstimated = estimated,
            byHolding = byHolding
        )
    }
}
