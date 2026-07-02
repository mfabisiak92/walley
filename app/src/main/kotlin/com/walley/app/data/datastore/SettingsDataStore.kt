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

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")

    val darkModeOverride: Flow<Boolean?> = context.settingsDataStore.data
        .map { preferences -> preferences[darkModeKey] }

    suspend fun setDarkModeOverride(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[darkModeKey] = enabled }
    }
}
