package com.walley.app.data.repository

import com.walley.app.data.datastore.AccountsPreferencesDataStore
import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.SortDirection
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class AccountsPreferencesRepositoryImpl @Inject constructor(
    private val accountsPreferencesDataStore: AccountsPreferencesDataStore
) : AccountsPreferencesRepository {

    override fun observeSort(): Flow<AccountsSortState> = accountsPreferencesDataStore.sortState

    override suspend fun setSortField(field: AccountSortField) {
        accountsPreferencesDataStore.setSortField(field)
    }

    override suspend fun setSortDirection(direction: SortDirection) {
        accountsPreferencesDataStore.setSortDirection(direction)
    }

    override suspend fun resetSort() {
        accountsPreferencesDataStore.resetSort()
    }

    override fun observeFilter(group: AccountBalanceGroup): Flow<AccountsFilterState> =
        accountsPreferencesDataStore.filterState(group)

    override suspend fun setStatusFilter(group: AccountBalanceGroup, status: AccountStatusFilter) {
        accountsPreferencesDataStore.setStatusFilter(group, status)
    }

    override suspend fun toggleCurrencyFilter(group: AccountBalanceGroup, currency: Currency) {
        accountsPreferencesDataStore.toggleCurrencyFilter(group, currency)
    }

    override suspend fun toggleTypeFilter(group: AccountBalanceGroup, type: AccountType) {
        accountsPreferencesDataStore.toggleTypeFilter(group, type)
    }

    override suspend fun setKindFilter(group: AccountBalanceGroup, kind: AccountKindFilter) {
        accountsPreferencesDataStore.setKindFilter(group, kind)
    }

    override suspend fun resetFilters(group: AccountBalanceGroup) {
        accountsPreferencesDataStore.resetFilters(group)
    }
}
