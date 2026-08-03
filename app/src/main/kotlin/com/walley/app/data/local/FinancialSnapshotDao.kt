package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialSnapshotDao {
    @Query("SELECT * FROM financial_snapshots ORDER BY year ASC, month ASC")
    fun observeAll(): Flow<List<FinancialSnapshotEntity>>

    @Query(
        """
        SELECT * FROM financial_snapshots
        WHERE year < :year OR (year = :year AND month < :month)
        ORDER BY year DESC, month DESC
        LIMIT 1
        """
    )
    suspend fun getPrevious(year: Int, month: Int): FinancialSnapshotEntity?

    @Insert
    suspend fun insert(snapshot: FinancialSnapshotEntity): Long

    @Insert
    suspend fun insertAll(snapshots: List<FinancialSnapshotEntity>)
}
