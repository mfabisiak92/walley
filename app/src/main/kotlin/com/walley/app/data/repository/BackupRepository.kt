package com.walley.app.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.walley.app.data.remote.DriveFile
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    fun observeAutoBackupEnabled(): Flow<Boolean>
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    fun observeLastBackupAt(): Flow<Instant?>
    fun observeSignedInAccountEmail(): Flow<String?>

    /** Signs in via Credential Manager and returns the account's email. */
    suspend fun signIn(activityContext: Context): String

    /** Requests (or silently renews) Drive access; may require launching [AuthorizationResult.pendingIntent] for consent. */
    suspend fun requestDriveAuthorization(): AuthorizationResult
    fun authorizationResultFromIntent(intent: Intent): AuthorizationResult

    suspend fun listSnapshots(accessToken: String): List<DriveFile>
    suspend fun backupNow(accessToken: String)
    suspend fun restoreSnapshot(accessToken: String, fileId: String)
}
