package com.walley.app.feature.investments

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * [change] as a percentage of [before], rounded to exactly 2 decimal places; null when [before] is
 * zero (percentage is undefined, not just large). Divides at extra precision first so the final
 * rounding to 2 places is a single, direct rounding rather than compounding two separate roundings.
 */
private fun percentChange(change: BigDecimal, before: BigDecimal): BigDecimal? =
    if (before.signum() == 0) null
    else (change.divide(before, 6, RoundingMode.HALF_UP) * BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)

/**
 * Summary of balance changes for a single investment within an account.
 */
data class InvestmentBalanceChange(
    val investmentId: Long,
    val name: String,
    val ticker: String,
    val beforeBalance: BigDecimal,
    val afterBalance: BigDecimal
) {
    val change: BigDecimal get() = afterBalance - beforeBalance
    val changePercent: BigDecimal? get() = percentChange(change, beforeBalance)
}

/**
 * Summary of balance changes for a single investment account.
 */
data class AccountBalanceChange(
    val accountId: Long,
    val accountName: String,
    val accountCurrencySymbol: String,
    val beforeAccountBalance: BigDecimal,
    val afterAccountBalance: BigDecimal,
    val beforeNetBalance: BigDecimal,
    val afterNetBalance: BigDecimal,
    val investments: List<InvestmentBalanceChange>
) {
    val accountChange: BigDecimal get() = afterAccountBalance - beforeAccountBalance
    val accountChangePercent: BigDecimal? get() = percentChange(accountChange, beforeAccountBalance)

    val netChange: BigDecimal get() = afterNetBalance - beforeNetBalance
    val netChangePercent: BigDecimal? get() = percentChange(netChange, beforeNetBalance)
}

/**
 * Sum of [AccountBalanceChange]s sharing one currency — accounts in different currencies are never
 * added together (same convention as elsewhere in the app, e.g. HomeViewModel's currencyTotals),
 * since that would require a conversion rate rather than a straight sum.
 */
data class CurrencyTotals(
    val currencySymbol: String,
    val beforeBalance: BigDecimal,
    val afterBalance: BigDecimal,
    val beforeNetBalance: BigDecimal,
    val afterNetBalance: BigDecimal
) {
    val balanceChange: BigDecimal get() = afterBalance - beforeBalance
    val balanceChangePercent: BigDecimal? get() = percentChange(balanceChange, beforeBalance)

    val netChange: BigDecimal get() = afterNetBalance - beforeNetBalance
    val netChangePercent: BigDecimal? get() = percentChange(netChange, beforeNetBalance)
}

/**
 * Complete preview of all price changes and their balance impacts.
 */
data class PriceUpdateReview(
    val accountChanges: List<AccountBalanceChange>,
    val priceChanges: Map<Long, BigDecimal> // investmentId to newPrice
) {
    /** One entry per currency present among [accountChanges], for a top-of-screen summary tile. */
    val totalsByCurrency: List<CurrencyTotals> by lazy {
        accountChanges
            .groupBy { it.accountCurrencySymbol }
            .map { (symbol, changes) ->
                CurrencyTotals(
                    currencySymbol = symbol,
                    beforeBalance = changes.sumOf { it.beforeAccountBalance },
                    afterBalance = changes.sumOf { it.afterAccountBalance },
                    beforeNetBalance = changes.sumOf { it.beforeNetBalance },
                    afterNetBalance = changes.sumOf { it.afterNetBalance }
                )
            }
    }
}
