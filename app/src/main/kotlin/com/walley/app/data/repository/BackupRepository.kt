package com.walley.app.data.repository

import android.net.Uri
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    fun observeLastBackupAt(): Flow<Instant?>
    suspend fun setLastBackupAt(instant: Instant)

    /**
     * Create and encrypt a backup snapshot, writing it to the given file URI.
     * Updates [observeLastBackupAt] upon successful completion.
     */
    suspend fun backupNow(fileUri: Uri)

    /**
     * Read and decrypt a backup snapshot from the given file URI,
     * then restore it to the local database (destructive operation).
     */
    suspend fun restoreSnapshot(fileUri: Uri)
}
