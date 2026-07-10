package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.walley.app.domain.model.BudgetItemIcon
import kotlinx.coroutines.flow.Flow

@Dao
interface AdHocBudgetDao {
    @Query("SELECT * FROM adhoc_budgets ORDER BY startDate DESC")
    fun observeBudgets(): Flow<List<AdHocBudgetEntity>>

    @Query("SELECT * FROM adhoc_budgets WHERE id = :budgetId")
    fun observeBudgetById(budgetId: Long): Flow<AdHocBudgetEntity?>

    @Query("SELECT * FROM adhoc_budget_items")
    fun observeAllItems(): Flow<List<AdHocBudgetItemEntity>>

    @Query("SELECT * FROM adhoc_budget_items WHERE budgetId = :budgetId")
    fun observeItemsForBudget(budgetId: Long): Flow<List<AdHocBudgetItemEntity>>

    @Query("SELECT * FROM adhoc_budget_items WHERE id = :itemId")
    suspend fun getItem(itemId: Long): AdHocBudgetItemEntity

    @Insert
    suspend fun insertBudget(budget: AdHocBudgetEntity): Long

    @Insert
    suspend fun insertItems(items: List<AdHocBudgetItemEntity>)

    @Insert
    suspend fun insertItem(item: AdHocBudgetItemEntity): Long

    @Query("UPDATE adhoc_budget_items SET paidAmountMinorUnits = :paidAmountMinorUnits WHERE id = :itemId")
    suspend fun updateItemPaidAmount(itemId: Long, paidAmountMinorUnits: Long)

    @Query("UPDATE adhoc_budget_items SET amountMinorUnits = :amountMinorUnits WHERE id = :itemId")
    suspend fun updateItemAmount(itemId: Long, amountMinorUnits: Long)

    @Query("UPDATE adhoc_budget_items SET icon = :icon WHERE id = :itemId")
    suspend fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?)

    @Query("DELETE FROM adhoc_budget_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM adhoc_budget_items WHERE budgetId = :budgetId")
    suspend fun deleteItemsForBudget(budgetId: Long)

    @Query("DELETE FROM adhoc_budgets WHERE id = :budgetId")
    suspend fun deleteBudget(budgetId: Long)

    @Query("UPDATE adhoc_budgets SET isCompleted = 1 WHERE id = :budgetId")
    suspend fun markCompleted(budgetId: Long)

    @Query("SELECT COUNT(*) FROM adhoc_budgets WHERE accountId = :accountId AND isCompleted = 0")
    suspend fun countActiveForAccount(accountId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM adhoc_budget_items " +
            "INNER JOIN adhoc_budgets ON adhoc_budget_items.budgetId = adhoc_budgets.id " +
            "WHERE adhoc_budget_items.accountId = :accountId AND adhoc_budgets.isCompleted = 0"
    )
    suspend fun countActiveItemsForAccount(accountId: Long): Int
}
