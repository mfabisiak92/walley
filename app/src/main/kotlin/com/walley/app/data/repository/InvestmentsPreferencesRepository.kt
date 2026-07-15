package com.walley.app.data.repository

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentSortField
import com.walley.app.domain.model.InvestmentsFilterState
import com.walley.app.domain.model.InvestmentsSortState
import com.walley.app.domain.model.PositionStatusFilter
import com.walley.app.domain.model.SortDirection
import kotlinx.coroutines.flow.Flow

interface InvestmentsPreferencesRepository {
    fun observeSort(): Flow<InvestmentsSortState>
    suspend fun setSortField(field: InvestmentSortField)
    suspend fun setSortDirection(direction: SortDirection)
    suspend fun resetSort()

    fun observeFilter(): Flow<InvestmentsFilterState>
    suspend fun setStatusFilter(status: PositionStatusFilter)
    suspend fun toggleCategoryFilter(category: InvestmentCategory)
    suspend fun toggleCurrencyFilter(currency: Currency)
    suspend fun toggleAccountFilter(accountId: Long)
    suspend fun resetFilters()
}
