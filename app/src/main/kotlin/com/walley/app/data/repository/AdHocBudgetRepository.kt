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
        items: List<AdHocBudgetItem>,
        applyAccountEffects: Boolean
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

    suspend fun deleteAdHocBudget(budgetId: Long)

    /**
     * Toggles whether paying/editing/un-paying an item in this budget moves money in its linked
     * account. Only affects future actions — balance changes already applied are left as-is.
     */
    suspend fun updateApplyAccountEffects(budgetId: Long, enabled: Boolean)
}
