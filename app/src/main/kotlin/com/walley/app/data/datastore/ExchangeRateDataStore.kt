package com.walley.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.walley.app.data.remote.FrankfurterResponse
import com.walley.app.domain.model.Currency
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.exchangeRateDataStore: DataStore<Preferences> by androidx.datastore.preferences.preferencesDataStore(name = "exchange_rates")

data class CachedRates(
    val response: FrankfurterResponse,
    val fetchedAtMillis: Long
)

class ExchangeRateDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private fun responseKey(base: Currency) = stringPreferencesKey("rates_json_${base.name}")
    private fun fetchedAtKey(base: Currency) = longPreferencesKey("rates_fetched_at_${base.name}")

    suspend fun read(base: Currency): CachedRates? {
        val preferences = context.exchangeRateDataStore.data.first()
        val body = preferences[responseKey(base)] ?: return null
        val fetchedAt = preferences[fetchedAtKey(base)] ?: return null
        return runCatching {
            CachedRates(json.decodeFromString<FrankfurterResponse>(body), fetchedAt)
        }.getOrNull()
    }

    suspend fun write(base: Currency, response: FrankfurterResponse, fetchedAtMillis: Long) {
        context.exchangeRateDataStore.edit { preferences ->
            preferences[responseKey(base)] = json.encodeToString(FrankfurterResponse.serializer(), response)
            preferences[fetchedAtKey(base)] = fetchedAtMillis
        }
    }
}
