package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY name ASC")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Insert
    suspend fun insert(investment: InvestmentEntity): Long

    @Query(
        """
        UPDATE investments
        SET name = :name, ticker = :ticker, quantity = :quantity, price = :price, accountId = :accountId
        WHERE id = :investmentId
        """
    )
    suspend fun update(
        investmentId: Long,
        name: String,
        ticker: String,
        quantity: BigDecimal,
        price: BigDecimal,
        accountId: Long?
    )

    @Query("DELETE FROM investments WHERE id = :investmentId")
    suspend fun delete(investmentId: Long)

    @Query("UPDATE investments SET accountId = NULL WHERE accountId = :accountId")
    suspend fun clearAccountAssociation(accountId: Long)
}
