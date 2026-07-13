package com.walley.app.data.repository

import kotlinx.coroutines.flow.Flow

interface IntegrationsRepository {
    fun observeYahooFinanceEnabled(): Flow<Boolean>
    suspend fun setYahooFinanceEnabled(enabled: Boolean)
}
