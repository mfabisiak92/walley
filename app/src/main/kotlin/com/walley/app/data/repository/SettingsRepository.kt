package com.walley.app.data.repository

import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeDarkModeOverride(): Flow<Boolean?>
    suspend fun setDarkModeOverride(enabled: Boolean)
    fun observeBaseCurrency(): Flow<Currency>
    suspend fun setBaseCurrency(currency: Currency)

    /** Target % of disposable income for [section] (Fixed costs/Other costs/Savings/Investments only); null if unset. */
    fun observeCategoryTarget(section: BudgetSectionType): Flow<BigDecimal?>
    suspend fun setCategoryTarget(section: BudgetSectionType, percent: BigDecimal?)
}
