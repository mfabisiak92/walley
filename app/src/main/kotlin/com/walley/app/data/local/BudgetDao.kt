package com.walley.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.Currency
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

    @Query(
        "SELECT COUNT(*) FROM budgets WHERE year = :year AND month = :month " +
            "AND (:excludeBudgetId IS NULL OR id != :excludeBudgetId)"
    )
    suspend fun countForMonth(year: Int, month: Int, excludeBudgetId: Long?): Int

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

    @Query("UPDATE budget_items SET accountId = :accountId, name = :name, currency = :currency WHERE id = :itemId")
    suspend fun updateItemAccountNameAndCurrency(itemId: Long, accountId: Long, name: String, currency: Currency)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudget(budgetId: Long)

    @Query("DELETE FROM budget_items WHERE budgetId = :budgetId")
    suspend fun deleteItemsForBudget(budgetId: Long)

    @Query("DELETE FROM budget_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("UPDATE budgets SET status = :status WHERE id = :budgetId")
    suspend fun updateStatus(budgetId: Long, status: BudgetStatus)

    @Query("UPDATE budgets SET year = :year, month = :month, status = :status WHERE id = :budgetId")
    suspend fun updateYearMonthAndStatus(budgetId: Long, year: Int, month: Int, status: BudgetStatus)

    @Query("UPDATE budgets SET applyAccountEffects = :enabled WHERE id = :budgetId")
    suspend fun updateApplyAccountEffects(budgetId: Long, enabled: Boolean)

    @Transaction
    suspend fun replaceItems(budgetId: Long, items: List<BudgetItemEntity>) {
        deleteItemsForBudget(budgetId)
        insertItems(items)
    }
}
