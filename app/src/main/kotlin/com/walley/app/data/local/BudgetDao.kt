package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    fun observeBudgetById(budgetId: Long): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget_items")
    fun observeAllItems(): Flow<List<BudgetItemEntity>>

    @Query("SELECT * FROM budget_items WHERE budgetId = :budgetId")
    fun observeItemsForBudget(budgetId: Long): Flow<List<BudgetItemEntity>>

    @Query("SELECT * FROM budget_items WHERE id = :itemId")
    suspend fun getItem(itemId: Long): BudgetItemEntity

    @Query("SELECT COUNT(*) FROM budgets WHERE year = :year AND month = :month")
    suspend fun countForMonth(year: Int, month: Int): Int

    @Insert
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Insert
    suspend fun insertItems(items: List<BudgetItemEntity>)

    @Insert
    suspend fun insertItem(item: BudgetItemEntity): Long

    @Query("UPDATE budget_items SET paidAmountMinorUnits = :paidAmountMinorUnits WHERE id = :itemId")
    suspend fun updateItemPaidAmount(itemId: Long, paidAmountMinorUnits: Long)

    @Query("UPDATE budget_items SET amountMinorUnits = :amountMinorUnits WHERE id = :itemId")
    suspend fun updateItemAmount(itemId: Long, amountMinorUnits: Long)

    @Query("UPDATE budget_items SET icon = :icon WHERE id = :itemId")
    suspend fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?)

    @Query("UPDATE budget_items SET accountId = :accountId WHERE id = :itemId")
    suspend fun updateItemAccount(itemId: Long, accountId: Long?)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudget(budgetId: Long)

    @Query("DELETE FROM budget_items WHERE budgetId = :budgetId")
    suspend fun deleteItemsForBudget(budgetId: Long)

    @Query("DELETE FROM budget_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("UPDATE budgets SET status = :status WHERE id = :budgetId")
    suspend fun updateStatus(budgetId: Long, status: BudgetStatus)
}
