package com.walley.app.data.repository

import android.content.Context
import android.net.Uri
import com.walley.app.data.backup.BackupSerializer
import com.walley.app.data.backup.BackupSnapshot
import com.walley.app.data.crypto.BackupCrypto
import com.walley.app.data.datastore.BackupPreferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupSerializer: BackupSerializer,
    private val backupCrypto: BackupCrypto,
    private val preferences: BackupPreferencesDataStore
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeLastBackupAt(): Flow<Instant?> = preferences.lastBackupAt

    override suspend fun setLastBackupAt(instant: Instant) = preferences.setLastBackupAt(instant)

    override suspend fun backupNow(fileUri: Uri) {
        val dataKey = getOrCreateDataKey()
        val snapshot = backupSerializer.buildSnapshot()
        val plaintext = json.encodeToString(BackupSnapshot.serializer(), snapshot).toByteArray()
        val encrypted = backupCrypto.encrypt(dataKey, plaintext)

        context.contentResolver.openOutputStream(fileUri)?.use {
            it.write(encrypted)
        } ?: error("Failed to open output stream for backup file")

        preferences.setLastBackupAt(Instant.now())
    }

    override suspend fun restoreSnapshot(fileUri: Uri) {
        val encrypted = context.contentResolver.openInputStream(fileUri)?.use {
            it.readBytes()
        } ?: error("Failed to open input stream for restore file")

        val dataKey = preferences.wrappedDataKey.first()?.let {
            backupCrypto.unwrapDataKeyFromLocalStorage(it)
        } ?: error("No backup key found locally — data cannot be restored")

        val plaintext = backupCrypto.decrypt(dataKey, encrypted)
        val snapshot = json.decodeFromString(BackupSnapshot.serializer(), String(plaintext))
        backupSerializer.restoreSnapshot(snapshot)
    }

    private suspend fun getOrCreateDataKey(): ByteArray {
        preferences.wrappedDataKey.first()?.let { return backupCrypto.unwrapDataKeyFromLocalStorage(it) }

        val newKey = backupCrypto.generateDataKey()
        persistDataKeyLocally(newKey)
        return newKey
    }

    private suspend fun persistDataKeyLocally(rawKey: ByteArray) {
        preferences.setWrappedDataKey(backupCrypto.wrapDataKeyForLocalStorage(rawKey))
    }
}
