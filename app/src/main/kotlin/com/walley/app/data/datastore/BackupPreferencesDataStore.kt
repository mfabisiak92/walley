package com.walley.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup")

class BackupPreferencesDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val lastBackupEpochMillisKey = longPreferencesKey("last_backup_epoch_millis")
    private val autoBackupEnabledKey = booleanPreferencesKey("auto_backup_enabled")
    private val wrappedDataKeyKey = byteArrayPreferencesKey("wrapped_data_key")
    private val signedInAccountEmailKey = stringPreferencesKey("signed_in_account_email")

    val lastBackupAt: Flow<Instant?> = context.backupDataStore.data
        .map { preferences -> preferences[lastBackupEpochMillisKey]?.let(Instant::ofEpochMilli) }

    suspend fun setLastBackupAt(instant: Instant) {
        context.backupDataStore.edit { preferences -> preferences[lastBackupEpochMillisKey] = instant.toEpochMilli() }
    }

    val autoBackupEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { preferences -> preferences[autoBackupEnabledKey] ?: false }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { preferences -> preferences[autoBackupEnabledKey] = enabled }
    }

    /** The Keystore-wrapped copy of the backup data key, kept only so this device doesn't have to re-fetch it from Drive on every backup. */
    val wrappedDataKey: Flow<ByteArray?> = context.backupDataStore.data
        .map { preferences -> preferences[wrappedDataKeyKey] }

    suspend fun setWrappedDataKey(bytes: ByteArray) {
        context.backupDataStore.edit { preferences -> preferences[wrappedDataKeyKey] = bytes }
    }

    val signedInAccountEmail: Flow<String?> = context.backupDataStore.data
        .map { preferences -> preferences[signedInAccountEmailKey] }

    suspend fun setSignedInAccountEmail(email: String?) {
        context.backupDataStore.edit { preferences ->
            if (email == null) preferences.remove(signedInAccountEmailKey) else preferences[signedInAccountEmailKey] = email
        }
    }
}
