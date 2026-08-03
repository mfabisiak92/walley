package com.walley.app.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.walley.app.data.backup.BackupSerializer
import com.walley.app.data.backup.BackupSnapshot
import com.walley.app.data.crypto.BackupCrypto
import com.walley.app.data.datastore.BackupPreferencesDataStore
import com.walley.app.data.remote.DriveFile
import com.walley.app.data.remote.GoogleAuthManager
import com.walley.app.data.remote.GoogleDriveApi
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private const val KEY_FILE_NAME = "walley_backup.key"
private const val SNAPSHOT_FILE_PREFIX = "walley_backup_"
private const val SNAPSHOT_FILE_SUFFIX = ".json.enc"
private const val MAX_SNAPSHOTS_KEPT = 10

class BackupRepositoryImpl @Inject constructor(
    private val backupSerializer: BackupSerializer,
    private val backupCrypto: BackupCrypto,
    private val driveApi: GoogleDriveApi,
    private val authManager: GoogleAuthManager,
    private val preferences: BackupPreferencesDataStore
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeAutoBackupEnabled(): Flow<Boolean> = preferences.autoBackupEnabled

    override suspend fun setAutoBackupEnabled(enabled: Boolean) = preferences.setAutoBackupEnabled(enabled)

    override fun observeLastBackupAt(): Flow<Instant?> = preferences.lastBackupAt

    override fun observeSignedInAccountEmail(): Flow<String?> = preferences.signedInAccountEmail

    override suspend fun signIn(activityContext: Context): String {
        val email = authManager.signIn(activityContext)
        preferences.setSignedInAccountEmail(email)
        return email
    }

    override suspend fun requestDriveAuthorization(): AuthorizationResult = authManager.requestDriveAuthorization()

    override fun authorizationResultFromIntent(intent: Intent): AuthorizationResult = authManager.resultFromIntent(intent)

    override suspend fun listSnapshots(accessToken: String): List<DriveFile> =
        driveApi.listFiles(accessToken).filter { it.name.startsWith(SNAPSHOT_FILE_PREFIX) }

    override suspend fun backupNow(accessToken: String) {
        val dataKey = getOrCreateDataKey(accessToken)
        val snapshot = backupSerializer.buildSnapshot()
        val plaintext = json.encodeToString(BackupSnapshot.serializer(), snapshot).toByteArray()
        val encrypted = backupCrypto.encrypt(dataKey, plaintext)

        driveApi.uploadFile(
            accessToken = accessToken,
            fileName = "$SNAPSHOT_FILE_PREFIX${System.currentTimeMillis()}$SNAPSHOT_FILE_SUFFIX",
            mimeType = "application/octet-stream",
            content = encrypted
        )

        pruneOldSnapshots(accessToken)
        preferences.setLastBackupAt(Instant.now())
    }

    override suspend fun restoreSnapshot(accessToken: String, fileId: String) {
        // Always fetched from Drive (not the local wrapped copy) since restore must work on a
        // fresh install where this device has never held the key.
        val dataKey = fetchDataKeyFromDrive(accessToken) ?: error("No backup key found in Drive — nothing to restore")
        val encrypted = driveApi.downloadFile(accessToken, fileId)
        val plaintext = backupCrypto.decrypt(dataKey, encrypted)
        val snapshot = json.decodeFromString(BackupSnapshot.serializer(), String(plaintext))
        backupSerializer.restoreSnapshot(snapshot)
        persistDataKeyLocally(dataKey)
    }

    private suspend fun getOrCreateDataKey(accessToken: String): ByteArray {
        preferences.wrappedDataKey.first()?.let { return backupCrypto.unwrapDataKeyFromLocalStorage(it) }

        fetchDataKeyFromDrive(accessToken)?.let { remoteKey ->
            persistDataKeyLocally(remoteKey)
            return remoteKey
        }

        val newKey = backupCrypto.generateDataKey()
        driveApi.uploadFile(accessToken, KEY_FILE_NAME, "application/octet-stream", newKey)
        persistDataKeyLocally(newKey)
        return newKey
    }

    private suspend fun fetchDataKeyFromDrive(accessToken: String): ByteArray? {
        val keyFile = driveApi.listFiles(accessToken).find { it.name == KEY_FILE_NAME } ?: return null
        return driveApi.downloadFile(accessToken, keyFile.id)
    }

    private suspend fun persistDataKeyLocally(rawKey: ByteArray) {
        preferences.setWrappedDataKey(backupCrypto.wrapDataKeyForLocalStorage(rawKey))
    }

    private suspend fun pruneOldSnapshots(accessToken: String) {
        val snapshots = listSnapshots(accessToken).sortedByDescending { it.createdTime }
        snapshots.drop(MAX_SNAPSHOTS_KEPT).forEach { driveApi.deleteFile(accessToken, it.id) }
    }
}
