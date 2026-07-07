package com.walley.app.data.repository

import com.walley.app.domain.model.FinancialSnapshot
import kotlinx.coroutines.flow.Flow

interface SnapshotRepository {
    fun observeSnapshots(): Flow<List<FinancialSnapshot>>
    /** The most recent snapshot strictly before [year]/[month], if any. */
    suspend fun previousSnapshot(year: Int, month: Int): FinancialSnapshot?
    suspend fun addSnapshot(snapshot: FinancialSnapshot)
}
