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

    @Query("SELECT * FROM investments WHERE id = :investmentId")
    suspend fun findById(investmentId: Long): InvestmentEntity?

    @Query("SELECT * FROM investment_transactions")
    fun observeAllTransactions(): Flow<List<InvestmentTransactionEntity>>

    @Query("SELECT * FROM investment_transactions WHERE investmentId = :investmentId ORDER BY date DESC, id DESC")
    fun observeTransactionsForInvestment(investmentId: Long): Flow<List<InvestmentTransactionEntity>>

    @Insert
    suspend fun insert(investment: InvestmentEntity): Long

    @Insert
    suspend fun insertAll(investments: List<InvestmentEntity>): List<Long>

    @Insert
    suspend fun insertTransaction(transaction: InvestmentTransactionEntity): Long

    @Insert
    suspend fun insertTransactions(transactions: List<InvestmentTransactionEntity>)

    @Query(
        """
        UPDATE investments
        SET name = :name, ticker = :ticker, category = :category, accountId = :accountId, exchange = :externalTicker
        WHERE id = :investmentId
        """
    )
    suspend fun update(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        accountId: Long?,
        externalTicker: String?
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

    // previousPrice is set from the row's current (pre-update) currentPrice, so it always holds
    // whatever price was in effect just before this call — the SET clauses all read the row's
    // pre-update values, so this is an atomic shift rather than a read-then-write.
    @Query(
        """
        UPDATE investments
        SET previousPrice = currentPrice, currentPrice = :currentPrice, lastPriceUpdate = :lastPriceUpdate
        WHERE id = :investmentId
        """
    )
    suspend fun updateCurrentPrice(investmentId: Long, currentPrice: BigDecimal, lastPriceUpdate: LocalDate)

    // Used instead of [updateCurrentPrice] when the "new" price is numerically the same as what's
    // already stored (e.g. re-saving the dialog unchanged, or a market refresh confirming the price
    // hasn't moved) — still marks the price as freshly checked without shifting the unchanged value
    // into previousPrice, which would otherwise stomp on the real previous price.
    @Query("UPDATE investments SET lastPriceUpdate = :lastPriceUpdate WHERE id = :investmentId")
    suspend fun touchLastPriceUpdate(investmentId: Long, lastPriceUpdate: LocalDate)

    // Swaps currentPrice and previousPrice — reverting is just "set the price to what it was
    // before", which itself is a price update, so the value being reverted from becomes the new
    // previousPrice. That makes a second revert a redo back to the original price. Guarded by
    // previousPrice IS NOT NULL so calling this with nothing to revert to is a harmless no-op.
    @Query(
        """
        UPDATE investments
        SET currentPrice = previousPrice, previousPrice = currentPrice, lastPriceUpdate = :lastPriceUpdate
        WHERE id = :investmentId AND previousPrice IS NOT NULL
        """
    )
    suspend fun revertToPreviousPrice(investmentId: Long, lastPriceUpdate: LocalDate)

    @Query("DELETE FROM investments WHERE id = :investmentId")
    suspend fun delete(investmentId: Long)

    @Query("DELETE FROM investment_transactions WHERE investmentId = :investmentId")
    suspend fun deleteTransactionsForInvestment(investmentId: Long)

    @Query("DELETE FROM investment_transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: Long)

    @Query("DELETE FROM strategy_investment_links WHERE investmentId = :investmentId")
    suspend fun deleteStrategyLinksForInvestment(investmentId: Long)

    @Transaction
    suspend fun deleteInvestmentWithTransactions(investmentId: Long) {
        deleteTransactionsForInvestment(investmentId)
        deleteStrategyLinksForInvestment(investmentId)
        delete(investmentId)
    }

    @Query("UPDATE investments SET accountId = NULL WHERE accountId = :accountId")
    suspend fun clearAccountAssociation(accountId: Long)

    @Query("SELECT COUNT(*) FROM investments WHERE accountId = :accountId")
    suspend fun countForAccount(accountId: Long): Int
}
