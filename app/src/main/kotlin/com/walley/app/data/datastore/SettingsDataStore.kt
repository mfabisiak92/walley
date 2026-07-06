package com.walley.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.walley.app.domain.model.Currency
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val baseCurrencyKey = stringPreferencesKey("base_currency")

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
}
