package com.walley.app.feature.accounts

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.SortDirection
import com.walley.app.feature.budget.convertToCurrency

fun filterAccounts(accounts: List<Account>, filter: AccountsFilterState): List<Account> = accounts.filter { account ->
    val statusMatches = when (filter.status) {
        AccountStatusFilter.ACTIVE -> !account.isClosed
        AccountStatusFilter.CLOSED -> account.isClosed
        AccountStatusFilter.ALL -> true
    }
    val currencyMatches = filter.currencies.isEmpty() || account.currency in filter.currencies
    val typeMatches = filter.types.isEmpty() || account.type in filter.types
    val kindMatches = when (filter.kind) {
        AccountKindFilter.ALL -> true
        AccountKindFilter.REAL -> !account.isVirtual
        AccountKindFilter.VIRTUAL -> account.isVirtual
    }
    statusMatches && currencyMatches && typeMatches && kindMatches
}

/**
 * [rates] converts each account's balance to [baseCurrency] for [AccountSortField.BALANCE], falling back
 * to the account's own (unconverted) balance when a rate is missing — keeps a mixed-currency list sorted
 * deterministically instead of throwing, at the cost of an imprecise order until rates are available.
 */
fun sortAccounts(accounts: List<Account>, sort: AccountsSortState, baseCurrency: Currency, rates: ExchangeRates?): List<Account> {
    val comparator: Comparator<Account> = when (sort.field) {
        AccountSortField.NAME -> compareBy { it.name.lowercase() }
        AccountSortField.BALANCE -> compareBy { convertToCurrency(it.balance, it.currency, baseCurrency, rates) ?: it.balance }
        AccountSortField.DATE_ADDED -> compareBy { it.id }
        AccountSortField.DEFAULT_FIRST -> compareByDescending<Account> { it.isDefault }.thenBy { it.name.lowercase() }
    }
    // Default-first has no direction to flip — it's a pin, not an ordering.
    val oriented = if (sort.field != AccountSortField.DEFAULT_FIRST && sort.direction == SortDirection.DESC) {
        comparator.reversed()
    } else {
        comparator
    }
    return accounts.sortedWith(oriented)
}
