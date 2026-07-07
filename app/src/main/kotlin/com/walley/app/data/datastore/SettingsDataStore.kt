package com.walley.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val baseCurrencyKey = stringPreferencesKey("base_currency")

    private val categoryTargetKeys = mapOf(
        BudgetSectionType.FIXED_COSTS to doublePreferencesKey("fixed_costs_target_percent"),
        BudgetSectionType.OTHER_COSTS to doublePreferencesKey("other_costs_target_percent"),
        BudgetSectionType.SAVINGS to doublePreferencesKey("savings_target_percent"),
        BudgetSectionType.INVESTMENTS to doublePreferencesKey("investments_target_percent")
    )

    val darkModeOverride: Flow<Boolean?> = context.settingsDataStore.data
        .map { preferences -> preferences[darkModeKey] }

    val baseCurrency: Flow<Currency> = context.settingsDataStore.data
        .map { preferences ->
            preferences[baseCurrencyKey]?.let { stored ->
                Currency.entries.find { it.name == stored }
            } ?: Currency.PLN
        }

    suspend fun setDarkModeOverride(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[darkModeKey] = enabled }
    }

    suspend fun setBaseCurrency(currency: Currency) {
        context.settingsDataStore.edit { preferences -> preferences[baseCurrencyKey] = currency.name }
    }

    fun categoryTarget(section: BudgetSectionType): Flow<BigDecimal?> {
        val key = categoryTargetKeys.getValue(section)
        return context.settingsDataStore.data.map { preferences -> preferences[key]?.let { BigDecimal.valueOf(it) } }
    }

    suspend fun setCategoryTarget(section: BudgetSectionType, percent: BigDecimal?) {
        val key = categoryTargetKeys.getValue(section)
        context.settingsDataStore.edit { preferences ->
            if (percent == null) preferences.remove(key) else preferences[key] = percent.toDouble()
        }
    }
}
