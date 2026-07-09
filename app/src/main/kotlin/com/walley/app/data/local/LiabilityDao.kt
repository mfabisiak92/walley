package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LiabilityDao {
    @Query("SELECT * FROM liabilities ORDER BY name ASC")
    fun observeAll(): Flow<List<LiabilityEntity>>

    @Insert
    suspend fun insert(liability: LiabilityEntity): Long

    @Query("UPDATE liabilities SET currentBalanceMinorUnits = :currentBalanceMinorUnits WHERE id = :liabilityId")
    suspend fun updateCurrentBalance(liabilityId: Long, currentBalanceMinorUnits: Long)

    @Query(
        "UPDATE liabilities SET originalAmountMinorUnits = :amountMinorUnits, " +
            "currentBalanceMinorUnits = :amountMinorUnits WHERE id = :liabilityId"
    )
    suspend fun resyncOriginalAndCurrentAmount(liabilityId: Long, amountMinorUnits: Long)

    @Query("DELETE FROM liabilities WHERE id = :liabilityId")
    suspend fun delete(liabilityId: Long)
}
