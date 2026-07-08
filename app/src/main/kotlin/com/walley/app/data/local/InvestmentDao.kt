package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.walley.app.domain.model.InvestmentCategory
import java.math.BigDecimal
import java.time.LocalDate
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
        SET name = :name, ticker = :ticker, category = :category, purchaseDate = :purchaseDate,
            quantity = :quantity, price = :price, currentPrice = :currentPrice, accountId = :accountId
        WHERE id = :investmentId
        """
    )
    suspend fun update(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        purchaseDate: LocalDate,
        quantity: BigDecimal,
        price: BigDecimal,
        currentPrice: BigDecimal,
        accountId: Long?
    )

    @Query("UPDATE investments SET currentPrice = :currentPrice WHERE id = :investmentId")
    suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal)

    @Query("DELETE FROM investments WHERE id = :investmentId")
    suspend fun delete(investmentId: Long)

    @Query("UPDATE investments SET accountId = NULL WHERE accountId = :accountId")
    suspend fun clearAccountAssociation(accountId: Long)

    @Query("SELECT COUNT(*) FROM investments WHERE accountId = :accountId")
    suspend fun countForAccount(accountId: Long): Int
}
