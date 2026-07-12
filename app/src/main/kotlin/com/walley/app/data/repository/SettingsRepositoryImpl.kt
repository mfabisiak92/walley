package com.walley.app.data.repository

import com.walley.app.data.datastore.SettingsDataStore
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeDarkModeOverride(): Flow<Boolean?> = settingsDataStore.darkModeOverride

    override suspend fun setDarkModeOverride(enabled: Boolean) {
        settingsDataStore.setDarkModeOverride(enabled)
    }

    override fun observeBaseCurrency(): Flow<Currency> = settingsDataStore.baseCurrency

    override suspend fun setBaseCurrency(currency: Currency) {
        settingsDataStore.setBaseCurrency(currency)
    }

    override fun observeIncludeSavingsInNetWorth(): Flow<Boolean> = settingsDataStore.includeSavingsInNetWorth

    override suspend fun setIncludeSavingsInNetWorth(enabled: Boolean) {
        settingsDataStore.setIncludeSavingsInNetWorth(enabled)
    }

    override fun observeCategoryTarget(section: BudgetSectionType): Flow<BigDecimal?> =
        settingsDataStore.categoryTarget(section)

    override suspend fun setCategoryTarget(section: BudgetSectionType, percent: BigDecimal?) {
        settingsDataStore.setCategoryTarget(section, percent)
    }
}
