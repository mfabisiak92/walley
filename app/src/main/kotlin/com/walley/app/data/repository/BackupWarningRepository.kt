package com.walley.app.data.repository

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BackupWarningRepository @Inject constructor(
    private val backupRepository: BackupRepository
) {

    fun shouldShowBackupWarning(): Flow<Boolean> = backupRepository.observeLastBackupAt().map { lastBackupAt ->
        val today = LocalDate.now()
        val isLastDayOfMonth = today == today.withDayOfMonth(today.lengthOfMonth())
        val lastBackupYearMonth = lastBackupAt?.let {
            val date = it.atZone(ZoneId.systemDefault()).toLocalDate()
            YearMonth.from(date)
        }
        val currentYearMonth = YearMonth.from(today)

        isLastDayOfMonth && lastBackupYearMonth != currentYearMonth
    }
}
