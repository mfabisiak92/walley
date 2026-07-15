package com.walley.app.domain.model

enum class InvestmentSortField { NAME, VALUE, GAIN_LOSS_PERCENT, DATE_ADDED }

/** A single flat portfolio list (unlike Accounts' three tabs), so both sort and filter are global here. */
data class InvestmentsSortState(
    val field: InvestmentSortField = InvestmentSortField.NAME,
    val direction: SortDirection = SortDirection.ASC
) {
    val isDefault: Boolean
        get() = this.field == InvestmentSortField.NAME && direction == SortDirection.ASC
}

enum class PositionStatusFilter { OPEN, CLOSED, ALL }

data class InvestmentsFilterState(
    val status: PositionStatusFilter = PositionStatusFilter.OPEN,
    val categories: Set<InvestmentCategory> = emptySet(),
    val currencies: Set<Currency> = emptySet(),
    val accountIds: Set<Long> = emptySet()
) {
    val isDefault: Boolean
        get() = status == PositionStatusFilter.OPEN && categories.isEmpty() && currencies.isEmpty() && accountIds.isEmpty()
}
