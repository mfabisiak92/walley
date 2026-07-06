package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY name ASC")
    fun observeAll(): Flow<List<AssetEntity>>

    @Insert
    suspend fun insert(asset: AssetEntity): Long

    @Query("UPDATE assets SET currentValueMinorUnits = :currentValueMinorUnits WHERE id = :assetId")
    suspend fun updateCurrentValue(assetId: Long, currentValueMinorUnits: Long)

    @Query("DELETE FROM assets WHERE id = :assetId")
    suspend fun delete(assetId: Long)
}
