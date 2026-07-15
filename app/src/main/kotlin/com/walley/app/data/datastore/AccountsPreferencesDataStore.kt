package com.walley.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.SortDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.accountsPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "accounts_preferences")

class AccountsPreferencesDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val sortFieldKey = stringPreferencesKey("sort_field")
    private val sortDirectionKey = stringPreferencesKey("sort_direction")

    private fun statusKey(group: AccountBalanceGroup) = stringPreferencesKey("filter_status_${group.name}")
    private fun currenciesKey(group: AccountBalanceGroup) = stringSetPreferencesKey("filter_currencies_${group.name}")
    private fun typesKey(group: AccountBalanceGroup) = stringSetPreferencesKey("filter_types_${group.name}")
    private fun kindKey(group: AccountBalanceGroup) = stringPreferencesKey("filter_kind_${group.name}")

    val sortState: Flow<AccountsSortState> = context.accountsPreferencesDataStore.data
        .map { preferences ->
            AccountsSortState(
                field = preferences[sortFieldKey]?.let { stored -> AccountSortField.entries.find { it.name == stored } }
                    ?: AccountSortField.NAME,
                direction = preferences[sortDirectionKey]?.let { stored -> SortDirection.entries.find { it.name == stored } }
                    ?: SortDirection.ASC
            )
        }

    suspend fun setSortField(field: AccountSortField) {
        context.accountsPreferencesDataStore.edit { preferences -> preferences[sortFieldKey] = field.name }
    }

    suspend fun setSortDirection(direction: SortDirection) {
        context.accountsPreferencesDataStore.edit { preferences -> preferences[sortDirectionKey] = direction.name }
    }

    suspend fun resetSort() {
        context.accountsPreferencesDataStore.edit { preferences ->
            preferences.remove(sortFieldKey)
            preferences.remove(sortDirectionKey)
        }
    }

    fun filterState(group: AccountBalanceGroup): Flow<AccountsFilterState> = context.accountsPreferencesDataStore.data
        .map { preferences ->
            AccountsFilterState(
                status = preferences[statusKey(group)]?.let { stored -> AccountStatusFilter.entries.find { it.name == stored } }
                    ?: AccountStatusFilter.ACTIVE,
                currencies = preferences[currenciesKey(group)]
                    ?.mapNotNull { stored -> Currency.entries.find { it.name == stored } }?.toSet()
                    ?: emptySet(),
                types = preferences[typesKey(group)]
                    ?.mapNotNull { stored -> AccountType.entries.find { it.name == stored } }?.toSet()
                    ?: emptySet(),
                kind = preferences[kindKey(group)]?.let { stored -> AccountKindFilter.entries.find { it.name == stored } }
                    ?: AccountKindFilter.ALL
            )
        }

    suspend fun setStatusFilter(group: AccountBalanceGroup, status: AccountStatusFilter) {
        context.accountsPreferencesDataStore.edit { preferences -> preferences[statusKey(group)] = status.name }
    }

    suspend fun toggleCurrencyFilter(group: AccountBalanceGroup, currency: Currency) {
        context.accountsPreferencesDataStore.edit { preferences ->
            val key = currenciesKey(group)
            val current = preferences[key] ?: emptySet()
            preferences[key] = if (currency.name in current) current - currency.name else current + currency.name
        }
    }

    suspend fun toggleTypeFilter(group: AccountBalanceGroup, type: AccountType) {
        context.accountsPreferencesDataStore.edit { preferences ->
            val key = typesKey(group)
            val current = preferences[key] ?: emptySet()
            preferences[key] = if (type.name in current) current - type.name else current + type.name
        }
    }

    suspend fun setKindFilter(group: AccountBalanceGroup, kind: AccountKindFilter) {
        context.accountsPreferencesDataStore.edit { preferences -> preferences[kindKey(group)] = kind.name }
    }

    suspend fun resetFilters(group: AccountBalanceGroup) {
        context.accountsPreferencesDataStore.edit { preferences ->
            preferences.remove(statusKey(group))
            preferences.remove(currenciesKey(group))
            preferences.remove(typesKey(group))
            preferences.remove(kindKey(group))
        }
    }
}
