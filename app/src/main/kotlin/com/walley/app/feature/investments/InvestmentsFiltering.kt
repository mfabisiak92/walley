package com.walley.app.feature.investments

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.InvestmentSortField
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.InvestmentsFilterState
import com.walley.app.domain.model.InvestmentsSortState
import com.walley.app.domain.model.PositionStatusFilter
import com.walley.app.domain.model.SortDirection
import com.walley.app.feature.budget.convertToCurrency
import java.math.BigDecimal

fun filterInvestments(investments: List<InvestmentWithTransactions>, filter: InvestmentsFilterState): List<InvestmentWithTransactions> =
    investments.filter { data ->
        val statusMatches = when (filter.status) {
            PositionStatusFilter.OPEN -> data.quantity.signum() != 0
            PositionStatusFilter.CLOSED -> data.quantity.signum() == 0
            PositionStatusFilter.ALL -> true
        }
        val categoryMatches = filter.categories.isEmpty() || data.investment.category in filter.categories
        val currencyMatches = filter.currencies.isEmpty() || data.investment.currency in filter.currencies
        val accountMatches = filter.accountIds.isEmpty() || data.investment.accountId in filter.accountIds
        statusMatches && categoryMatches && currencyMatches && accountMatches
    }

/**
 * [rates] converts each position's current value to [baseCurrency] for [InvestmentSortField.VALUE],
 * falling back to the position's own (unconverted) value when a rate is missing — same trade-off as
 * [com.walley.app.feature.accounts.sortAccounts]'s balance sort.
 */
fun sortInvestments(
    investments: List<InvestmentWithTransactions>,
    sort: InvestmentsSortState,
    baseCurrency: Currency,
    rates: ExchangeRates?
): List<InvestmentWithTransactions> {
    val comparator: Comparator<InvestmentWithTransactions> = when (sort.field) {
        InvestmentSortField.NAME -> compareBy { it.investment.name.lowercase() }
        InvestmentSortField.VALUE -> compareBy {
            convertToCurrency(it.currentValue, it.investment.currency, baseCurrency, rates) ?: it.currentValue
        }
        // Not yet meaningful (no open cost basis) sorts as flat/zero rather than dropping to the bottom or top.
        InvestmentSortField.GAIN_LOSS_PERCENT -> compareBy { it.unrealizedGainLossPercent ?: BigDecimal.ZERO }
        InvestmentSortField.DATE_ADDED -> compareBy { it.investment.id }
    }
    val oriented = if (sort.direction == SortDirection.DESC) comparator.reversed() else comparator
    return investments.sortedWith(oriented)
}
