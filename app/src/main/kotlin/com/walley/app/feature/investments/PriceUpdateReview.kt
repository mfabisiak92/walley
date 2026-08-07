package com.walley.app.feature.investments

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.InvestmentWithTransactions
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

/**
 * Computes each of [accounts]' balance/net-balance change for a hypothetical set of [priceUpdates]
 * (investment id to candidate new price), without writing anything. Pure and synchronous so it can
 * drive both [UpdatePricesViewModel.generateReview]'s saved review (computed once, from freshly
 * fetched data) and a live per-account preview header that recomputes on every keystroke — the caller
 * decides which accounts are in scope (e.g. one, when reached from a single account's Investments tab)
 * by what it passes in [accounts]; this function doesn't filter beyond investment type.
 */
fun computeAccountBalanceChanges(
    investmentsByAccount: Map<Long, List<InvestmentWithTransactions>>,
    accounts: List<Account>,
    priceUpdates: Map<Long, BigDecimal>
): List<AccountBalanceChange> =
    accounts.filter { it.type == AccountType.INVESTMENT }.mapNotNull { account ->
        val investmentsInAccount = investmentsByAccount[account.id].orEmpty()
        if (investmentsInAccount.isEmpty()) return@mapNotNull null

        var afterAccountValue = BigDecimal.ZERO
        val investmentChanges = investmentsInAccount.mapNotNull { invWithTx ->
            val investment = invWithTx.investment
            val newPrice = priceUpdates[investment.id] ?: investment.currentPrice
            val beforeValue = invWithTx.currentValue
            val afterValue = invWithTx.quantity * newPrice
            afterAccountValue += afterValue

            if (beforeValue.compareTo(afterValue) != 0) {
                InvestmentBalanceChange(
                    investmentId = investment.id,
                    name = investment.name,
                    ticker = investment.ticker,
                    beforeBalance = beforeValue,
                    afterBalance = afterValue
                )
            } else {
                null
            }
        }

        // account.balance/uninvestedCash/investmentCostBasis already reflect the current (pre-edit)
        // prices — repository.observeAccounts() folds live investment values into balance. Cost
        // basis is derived from purchase price, not current price, so it (and uninvestedCash) stays
        // put; only balance moves with the edited prices. A plain .copy(balance = ...) is therefore
        // enough to get the "after" account's own netWorthValue — which subtracts tax owed on the
        // unrealized gain — via the same formula Account already uses everywhere else in the app.
        //
        // Rounded the same way AccountRepositoryImpl.investmentsValue() rounds it for [account.balance]
        // itself — without this, an untouched account (no priceUpdates at all, or ones that don't
        // change anything) could still show a phantom fraction-of-a-cent "change" purely from comparing
        // an unrounded fresh sum against the already-rounded before value.
        val afterAccountBalance = account.uninvestedCash + afterAccountValue.setScale(2, RoundingMode.HALF_UP)
        val afterAccount = account.copy(balance = afterAccountBalance)

        AccountBalanceChange(
            accountId = account.id,
            accountName = account.name,
            accountCurrencySymbol = account.currency.symbol,
            beforeAccountBalance = account.balance,
            afterAccountBalance = afterAccountBalance,
            beforeNetBalance = account.netWorthValue,
            afterNetBalance = afterAccount.netWorthValue,
            investments = investmentChanges
        )
    }
