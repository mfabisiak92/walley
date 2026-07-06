package com.walley.app.data.repository

import com.walley.app.data.datastore.ExchangeRateDataStore
import com.walley.app.data.remote.FrankfurterApi
import com.walley.app.data.remote.toDomain
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ExchangeRateRepositoryImpl @Inject constructor(
    private val api: FrankfurterApi,
    private val cache: ExchangeRateDataStore
) : ExchangeRateRepository {

    override fun observeRates(base: Currency): Flow<ExchangeRates?> = flow {
        val cached = cache.read(base)
        if (cached != null) emit(cached.response.toDomain())

        val cacheIsFresh = cached != null &&
            System.currentTimeMillis() - cached.fetchedAtMillis < CACHE_TTL_MILLIS
        if (cacheIsFresh) return@flow

        val fresh = runCatching { api.fetchLatest(base) }.getOrNull()
        when {
            fresh != null -> {
                cache.write(base, fresh, System.currentTimeMillis())
                emit(fresh.toDomain())
            }
            cached == null -> emit(null)
        }
    }

    private companion object {
        const val CACHE_TTL_MILLIS = 60 * 60 * 1000L
    }
}
