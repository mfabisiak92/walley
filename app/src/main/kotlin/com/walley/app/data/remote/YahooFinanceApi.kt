package com.walley.app.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class YahooChartResponse(val chart: YahooChart)

@Serializable
data class YahooChart(val result: List<YahooChartResult>? = null, val error: YahooChartError? = null)

@Serializable
data class YahooChartResult(val meta: YahooChartMeta)

@Serializable
data class YahooChartMeta(
    val symbol: String? = null,
    val currency: String? = null,
    val regularMarketPrice: Double? = null
)

@Serializable
data class YahooChartError(val code: String? = null, val description: String? = null)

/**
 * Unofficial, undocumented endpoint (no key, no signup) — Yahoo doesn't publish terms for this usage,
 * so it's no more guaranteed to stay working than the Stooq endpoint this replaced. It does require a
 * browser-like User-Agent: requests with a default HTTP-client UA get 429'd almost immediately.
 */
class YahooFinanceApi @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchQuote(symbol: String): YahooChartResponse = withContext(Dispatchers.IO) {
        val encodedSymbol = URLEncoder.encode(symbol, "UTF-8")
        val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$encodedSymbol")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            // A non-2xx response's body (which carries the actual error message) is only readable via
            // errorStream — inputStream throws for those statuses, so reading it unconditionally would
            // turn every error (unknown symbol, rate limit, etc.) into an opaque failure.
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            json.decodeFromString<YahooChartResponse>(body)
        } finally {
            connection.disconnect()
        }
    }
}
