package com.walley.app.data.repository

import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetWithItems
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeBudgetsWithItems(): Flow<List<BudgetWithItems>>
    fun observeBudget(budgetId: Long): Flow<BudgetWithItems?>
    /** The Active/Completed budget for the given calendar month, if one exists; Drafts don't count. */
    fun observeBudgetForMonth(year: Int, month: Int): Flow<BudgetWithItems?>

    /** [excludeBudgetId] lets a budget being resumed/edited ignore its own row when checking. */
    suspend fun monthHasBudget(year: Int, month: Int, excludeBudgetId: Long? = null): Boolean

    /**
     * Persists the wizard's current progress as a Draft (creating it if [budgetId] is null, replacing
     * its items otherwise). Drafts aren't shown as Active and never auto-pay items. [items] should
     * carry section/name/amount/currency/accountId/paymentDay; id and budgetId are ignored.
     */
    suspend fun saveDraft(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        applyAccountEffects: Boolean
    ): Long

    /** Finalizes a budget as Active (Draft -&gt; Active if [budgetId] is given, otherwise a brand-new Active budget). */
    suspend fun submitBudget(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        applyAccountEffects: Boolean
    ): Long

    suspend fun markItemPaid(itemId: Long)
    suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal)

    /** Edits an item's planned amount; if it drops below the amount already paid, paidAmount is clamped down to match. */
    suspend fun updateItemAmount(itemId: Long, amount: BigDecimal)

    suspend fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?)

    /** Reassigns an item's linked account, reversing/reapplying any already-paid amount's balance effect. */
    suspend fun updateItemAccount(itemId: Long, accountId: Long?)

    /** @throws BudgetIsCompletedException if the budget's status is [com.walley.app.domain.model.BudgetStatus.COMPLETED]. */
    suspend fun deleteBudget(budgetId: Long)

    /** Deletes a single item, overriding the usual locked-after-creation restriction. */
    suspend fun deleteBudgetItem(itemId: Long)

    /** Re-inserts a previously deleted item (used to support "undo"), preserving its original id. */
    suspend fun restoreBudgetItem(item: BudgetItem)

    /** One-way transition marking a budget as completed; completed budgets can no longer be deleted. */
    suspend fun markBudgetCompleted(budgetId: Long)

    /**
     * Toggles whether paying/editing/un-paying an item in this budget moves money in its linked
     * account. Only affects future actions — balance changes already applied are left as-is.
     */
    suspend fun updateApplyAccountEffects(budgetId: Long, enabled: Boolean)

    /** Auto-marks items whose payment day has passed as paid, for Active budgets only; call when a budget screen opens. */
    suspend fun checkAndAutoCompleteDueItems()
}

class BudgetIsCompletedException : Exception("Budget is marked as completed and can't be deleted")
