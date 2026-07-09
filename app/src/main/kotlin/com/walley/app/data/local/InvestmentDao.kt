package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY name ASC")
    fun observeAll(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE id = :investmentId")
    fun observeById(investmentId: Long): Flow<InvestmentEntity?>

    @Query("SELECT * FROM investments WHERE accountId = :accountId AND ticker = :ticker LIMIT 1")
    suspend fun findByAccountAndTicker(accountId: Long, ticker: String): InvestmentEntity?

    @Query("SELECT * FROM investment_transactions")
    fun observeAllTransactions(): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions WHERE investmentId = :investmentId ORDER BY date DESC, id DESC")
    fun observeTransactionsForInvestment(investmentId: Long): Flow<List<InvestmentTransactionEntity>>

    @Insert
    suspend fun insert(investment: InvestmentEntity): Long

    @Insert
    suspend fun insertTransaction(transaction: InvestmentTransactionEntity): Long

    @Query(
        """
        UPDATE investments
        SET name = :name, ticker = :ticker, category = :category, accountId = :accountId
        WHERE id = :investmentId
        """
    )
    suspend fun update(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        accountId: Long?
    )

    @Query(
        """
        UPDATE investment_transactions
        SET type = :type, date = :date, quantity = :quantity, pricePerUnit = :pricePerUnit, commission = :commission
        WHERE id = :transactionId
        """
    )
    suspend fun updateTransaction(
        transactionId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal
    )

    @Query("UPDATE investments SET currentPrice = :currentPrice WHERE id = :investmentId")
    suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal)

    @Query("DELETE FROM investments WHERE id = :investmentId")
    suspend fun delete(investmentId: Long)

    @Query("DELETE FROM investment_transactions WHERE investmentId = :investmentId")
    suspend fun deleteTransactionsForInvestment(investmentId: Long)

    @Query("DELETE FROM investment_transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)

    @Transaction
    suspend fun deleteInvestmentWithTransactions(investmentId: Long) {
        deleteTransactionsForInvestment(investmentId)
        delete(investmentId)
    }

    @Query("UPDATE investments SET accountId = NULL WHERE accountId = :accountId")
    suspend fun clearAccountAssociation(accountId: Long)

    @Query("SELECT COUNT(*) FROM investments WHERE accountId = :accountId")
    suspend fun countForAccount(accountId: Long): Int
}
