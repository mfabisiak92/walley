package com.walley.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentSortField
import com.walley.app.domain.model.InvestmentsFilterState
import com.walley.app.domain.model.InvestmentsSortState
import com.walley.app.domain.model.PositionStatusFilter
import com.walley.app.domain.model.SortDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.investmentsPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "investments_preferences")

class InvestmentsPreferencesDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val sortFieldKey = stringPreferencesKey("sort_field")
    private val sortDirectionKey = stringPreferencesKey("sort_direction")
    private val statusKey = stringPreferencesKey("filter_status")
    private val categoriesKey = stringSetPreferencesKey("filter_categories")
    private val currenciesKey = stringSetPreferencesKey("filter_currencies")
    private val accountIdsKey = stringSetPreferencesKey("filter_account_ids")

    val sortState: Flow<InvestmentsSortState> = context.investmentsPreferencesDataStore.data
        .map { preferences ->
            InvestmentsSortState(
                field = preferences[sortFieldKey]?.let { stored -> InvestmentSortField.entries.find { it.name == stored } }
                    ?: InvestmentSortField.NAME,
                direction = preferences[sortDirectionKey]?.let { stored -> SortDirection.entries.find { it.name == stored } }
                    ?: SortDirection.ASC
            )
        }

    suspend fun setSortField(field: InvestmentSortField) {
        context.investmentsPreferencesDataStore.edit { preferences -> preferences[sortFieldKey] = field.name }
    }

    suspend fun setSortDirection(direction: SortDirection) {
        context.investmentsPreferencesDataStore.edit { preferences -> preferences[sortDirectionKey] = direction.name }
    }

    suspend fun resetSort() {
        context.investmentsPreferencesDataStore.edit { preferences ->
            preferences.remove(sortFieldKey)
            preferences.remove(sortDirectionKey)
        }
    }

    val filterState: Flow<InvestmentsFilterState> = context.investmentsPreferencesDataStore.data
        .map { preferences ->
            InvestmentsFilterState(
                status = preferences[statusKey]?.let { stored -> PositionStatusFilter.entries.find { it.name == stored } }
                    ?: PositionStatusFilter.OPEN,
                categories = preferences[categoriesKey]
                    ?.mapNotNull { stored -> InvestmentCategory.entries.find { it.name == stored } }?.toSet()
                    ?: emptySet(),
                currencies = preferences[currenciesKey]
                    ?.mapNotNull { stored -> Currency.entries.find { it.name == stored } }?.toSet()
                    ?: emptySet(),
                accountIds = preferences[accountIdsKey]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
            )
        }

    suspend fun setStatusFilter(status: PositionStatusFilter) {
        context.investmentsPreferencesDataStore.edit { preferences -> preferences[statusKey] = status.name }
    }

    suspend fun toggleCategoryFilter(category: InvestmentCategory) {
        context.investmentsPreferencesDataStore.edit { preferences ->
            val current = preferences[categoriesKey] ?: emptySet()
            preferences[categoriesKey] = if (category.name in current) current - category.name else current + category.name
        }
    }

    suspend fun toggleCurrencyFilter(currency: Currency) {
        context.investmentsPreferencesDataStore.edit { preferences ->
            val current = preferences[currenciesKey] ?: emptySet()
            preferences[currenciesKey] = if (currency.name in current) current - currency.name else current + currency.name
        }
    }

    suspend fun toggleAccountFilter(accountId: Long) {
        context.investmentsPreferencesDataStore.edit { preferences ->
            val id = accountId.toString()
            val current = preferences[accountIdsKey] ?: emptySet()
            preferences[accountIdsKey] = if (id in current) current - id else current + id
        }
    }

    suspend fun resetFilters() {
        context.investmentsPreferencesDataStore.edit { preferences ->
            preferences.remove(statusKey)
            preferences.remove(categoriesKey)
            preferences.remove(currenciesKey)
            preferences.remove(accountIdsKey)
        }
    }
}
