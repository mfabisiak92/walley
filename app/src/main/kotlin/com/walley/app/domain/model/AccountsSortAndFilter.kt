package com.walley.app.domain.model

enum class AccountSortField { NAME, BALANCE, DATE_ADDED, DEFAULT_FIRST }

enum class SortDirection { ASC, DESC }

/** Global across all Accounts tabs — sort order is a single standing preference, not per tab. */
data class AccountsSortState(
    val field: AccountSortField = AccountSortField.NAME,
    val direction: SortDirection = SortDirection.ASC
) {
    val isDefault: Boolean
        get() = this.field == AccountSortField.NAME && direction == SortDirection.ASC
}

enum class AccountStatusFilter { ACTIVE, CLOSED, ALL }

/** Only meaningful on the Savings tab, where real and virtual (earmarked) accounts are mixed together. */
enum class AccountKindFilter { ALL, REAL, VIRTUAL }

/**
 * Scoped per [AccountBalanceGroup] tab, since the fields that make sense differ per tab: [types] only
 * applies on Cash & Checking (the only tab mixing more than one [AccountType]), and [kind] only applies
 * on Savings (the only tab mixing real and virtual accounts).
 */
data class AccountsFilterState(
    val status: AccountStatusFilter = AccountStatusFilter.ACTIVE,
    val currencies: Set<Currency> = emptySet(),
    val types: Set<AccountType> = emptySet(),
    val kind: AccountKindFilter = AccountKindFilter.ALL
) {
    val isDefault: Boolean
        get() = status == AccountStatusFilter.ACTIVE && currencies.isEmpty() && types.isEmpty() && kind == AccountKindFilter.ALL
}
