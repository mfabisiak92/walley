package com.walley.app.data.repository

import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.AdHocBudgetWithItems
import com.walley.app.domain.model.BudgetItemIcon
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface AdHocBudgetRepository {
    fun observeAdHocBudgetsWithItems(): Flow<List<AdHocBudgetWithItems>>
    fun observeAdHocBudget(budgetId: Long): Flow<AdHocBudgetWithItems?>

    /** Creates the budget and all its items in one go; [items]' id/budgetId are ignored. */
    suspend fun createAdHocBudget(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long,
        items: List<AdHocBudgetItem>
    ): Long

    suspend fun markItemPaid(itemId: Long)
    suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal)

    /** Edits an item's planned amount; if it drops below the amount already paid, paidAmount is clamped down to match. */
    suspend fun updateItemAmount(itemId: Long, amount: BigDecimal)

    suspend fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?)

    /** Deletes a single item, without reversing any account balance change it already applied. */
    suspend fun deleteBudgetItem(itemId: Long)

    /** Re-inserts a previously deleted item (used to support "undo"), preserving its original id. */
    suspend fun restoreBudgetItem(item: AdHocBudgetItem)

    /** @throws BudgetIsCompletedException if the budget is already marked completed. */
    suspend fun deleteAdHocBudget(budgetId: Long)

    /** One-way: marks the budget completed, same as [BudgetRepository.markBudgetCompleted] does for monthly budgets. */
    suspend fun markCompleted(budgetId: Long)
}
