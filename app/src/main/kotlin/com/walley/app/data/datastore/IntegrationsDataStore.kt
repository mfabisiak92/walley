package com.walley.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.integrationsDataStore: DataStore<Preferences> by preferencesDataStore(name = "integrations")

class IntegrationsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val yahooFinanceEnabledKey = booleanPreferencesKey("yahoo_finance_enabled")

    val yahooFinanceEnabled: Flow<Boolean> = context.integrationsDataStore.data
        .map { preferences -> preferences[yahooFinanceEnabledKey] ?: false }

    suspend fun setYahooFinanceEnabled(enabled: Boolean) {
        context.integrationsDataStore.edit { preferences -> preferences[yahooFinanceEnabledKey] = enabled }
    }
}
