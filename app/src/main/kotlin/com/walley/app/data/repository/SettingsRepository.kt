package com.walley.app.data.repository

import com.walley.app.domain.model.Currency
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeDarkModeOverride(): Flow<Boolean?>
    suspend fun setDarkModeOverride(enabled: Boolean)
    fun observeBaseCurrency(): Flow<Currency>
    suspend fun setBaseCurrency(currency: Currency)
}
