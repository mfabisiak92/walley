package com.walley.app.data.repository

import com.walley.app.data.datastore.InvestmentsPreferencesDataStore
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentSortField
import com.walley.app.domain.model.InvestmentsFilterState
import com.walley.app.domain.model.InvestmentsSortState
import com.walley.app.domain.model.PositionStatusFilter
import com.walley.app.domain.model.SortDirection
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class InvestmentsPreferencesRepositoryImpl @Inject constructor(
    private val investmentsPreferencesDataStore: InvestmentsPreferencesDataStore
) : InvestmentsPreferencesRepository {

    override fun observeSort(): Flow<InvestmentsSortState> = investmentsPreferencesDataStore.sortState

    override suspend fun setSortField(field: InvestmentSortField) {
        investmentsPreferencesDataStore.setSortField(field)
    }

    override suspend fun setSortDirection(direction: SortDirection) {
        investmentsPreferencesDataStore.setSortDirection(direction)
    }

    override suspend fun resetSort() {
        investmentsPreferencesDataStore.resetSort()
    }

    override fun observeFilter(): Flow<InvestmentsFilterState> = investmentsPreferencesDataStore.filterState

    override suspend fun setStatusFilter(status: PositionStatusFilter) {
        investmentsPreferencesDataStore.setStatusFilter(status)
    }

    override suspend fun toggleCategoryFilter(category: InvestmentCategory) {
        investmentsPreferencesDataStore.toggleCategoryFilter(category)
    }

    override suspend fun toggleCurrencyFilter(currency: Currency) {
        investmentsPreferencesDataStore.toggleCurrencyFilter(currency)
    }

    override suspend fun toggleAccountFilter(accountId: Long) {
        investmentsPreferencesDataStore.toggleAccountFilter(accountId)
    }

    override suspend fun resetFilters() {
        investmentsPreferencesDataStore.resetFilters()
    }
}
