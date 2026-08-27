package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentPriceHistoryDao {
    @Query("SELECT * FROM investment_price_history ORDER BY investmentId, date")
    fun observeAll(): Flow<List<InvestmentPriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prices: List<InvestmentPriceHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(price: InvestmentPriceHistoryEntity)
}
