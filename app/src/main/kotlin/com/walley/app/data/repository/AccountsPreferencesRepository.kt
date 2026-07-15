package com.walley.app.data.repository

import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.SortDirection
import kotlinx.coroutines.flow.Flow

interface AccountsPreferencesRepository {
    fun observeSort(): Flow<AccountsSortState>
    suspend fun setSortField(field: AccountSortField)
    suspend fun setSortDirection(direction: SortDirection)
    suspend fun resetSort()

    fun observeFilter(group: AccountBalanceGroup): Flow<AccountsFilterState>
    suspend fun setStatusFilter(group: AccountBalanceGroup, status: AccountStatusFilter)
    suspend fun toggleCurrencyFilter(group: AccountBalanceGroup, currency: Currency)
    suspend fun toggleTypeFilter(group: AccountBalanceGroup, type: AccountType)
    suspend fun setKindFilter(group: AccountBalanceGroup, kind: AccountKindFilter)
    suspend fun resetFilters(group: AccountBalanceGroup)
}
