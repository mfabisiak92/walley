package com.walley.app.data.repository

import com.walley.app.data.datastore.IntegrationsDataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class IntegrationsRepositoryImpl @Inject constructor(
    private val integrationsDataStore: IntegrationsDataStore
) : IntegrationsRepository {

    override fun observeYahooFinanceEnabled(): Flow<Boolean> = integrationsDataStore.yahooFinanceEnabled

    override suspend fun setYahooFinanceEnabled(enabled: Boolean) {
        integrationsDataStore.setYahooFinanceEnabled(enabled)
    }
}
