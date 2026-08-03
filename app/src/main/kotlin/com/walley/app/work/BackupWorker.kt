package com.walley.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.walley.app.data.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val authorization = backupRepository.requestDriveAuthorization()
        val accessToken = authorization.accessToken

        // A background worker has no Activity to launch consent through — if the scope grant
        // needs interactive resolution (e.g. it was revoked), skip this run rather than crash;
        // the user completes it next time they open the Backup & Restore screen, and the next
        // scheduled run will pick up from there.
        if (authorization.hasResolution() || accessToken == null) return Result.failure()

        return try {
            backupRepository.backupNow(accessToken)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
