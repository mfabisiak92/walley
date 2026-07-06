package com.walley.app.data.remote

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class FrankfurterResponse(
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

class FrankfurterApi @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchLatest(base: Currency): FrankfurterResponse = withContext(Dispatchers.IO) {
        val symbols = Currency.entries.filter { it != base }.joinToString(",") { it.name }
        val url = URL("https://api.frankfurter.dev/v1/latest?base=${base.name}&symbols=$symbols")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<FrankfurterResponse>(body)
        } finally {
            connection.disconnect()
        }
    }
}

fun FrankfurterResponse.toDomain(): ExchangeRates = ExchangeRates(
    base = Currency.valueOf(base),
    rates = rates
        .filterKeys { key -> Currency.entries.any { it.name == key } }
        .map { (key, value) -> Currency.valueOf(key) to value.toBigDecimal() }
        .toMap(),
    date = date
)
