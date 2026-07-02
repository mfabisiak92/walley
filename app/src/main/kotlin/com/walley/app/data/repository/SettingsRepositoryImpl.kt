package com.walley.app.data.repository

import com.walley.app.data.datastore.SettingsDataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeDarkModeOverride(): Flow<Boolean?> = settingsDataStore.darkModeOverride

    override suspend fun setDarkModeOverride(enabled: Boolean) {
        settingsDataStore.setDarkModeOverride(enabled)
    }
}
