package com.walley.app.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeDarkModeOverride(): Flow<Boolean?>
    suspend fun setDarkModeOverride(enabled: Boolean)
}
