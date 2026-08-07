package com.walley.app.data.repository

import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.IncomeCategory
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
     * [plannedNetWorth] is nullable here (unlike [submitBudget]) since a draft can still be incomplete.
     */
    suspend fun saveDraft(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        applyIncomeAccountEffects: Boolean,
        applyCostsAccountEffects: Boolean,
        applySavingsAccountEffects: Boolean,
        applyInvestmentsAccountEffects: Boolean,
        plannedNetWorth: BigDecimal?
    ): Long

    /** Finalizes a budget as Active (Draft -&gt; Active if [budgetId] is given, otherwise a brand-new Active budget). */
    suspend fun submitBudget(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        applyIncomeAccountEffects: Boolean,
        applyCostsAccountEffects: Boolean,
        applySavingsAccountEffects: Boolean,
        applyInvestmentsAccountEffects: Boolean,
        plannedNetWorth: BigDecimal
    ): Long

    suspend fun markItemPaid(itemId: Long)
    suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal)

    /**
     * One-way lock for an Income/Income-related-expenses item: no-ops for any other section. Once
     * finalized, [markItemPaid]/[markItemPartiallyPaid]/[updateItemAmount] no longer touch this item.
     */
    suspend fun finalizeItem(itemId: Long)

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

    /** Adds a brand-new item to an already-persisted budget. Returns the new item's id. */
    suspend fun addBudgetItem(
        budgetId: Long,
        section: BudgetSectionType,
        name: String,
        amount: BigDecimal,
        currency: Currency,
        accountId: Long?,
        paymentDay: Int?,
        paymentDayIsLastOfMonth: Boolean,
        incomeCategory: IncomeCategory?,
        icon: BudgetItemIcon?
    ): Long

    /** One-way transition marking a budget as completed; completed budgets can no longer be deleted. */
    suspend fun markBudgetCompleted(budgetId: Long)

    /**
     * Toggles, per section group, whether paying/editing/un-paying an item moves money in its
     * linked account. Only affects future actions — balance changes already applied are left as-is.
     */
    suspend fun updateApplyIncomeAccountEffects(budgetId: Long, enabled: Boolean)
    suspend fun updateApplyCostsAccountEffects(budgetId: Long, enabled: Boolean)
    suspend fun updateApplySavingsAccountEffects(budgetId: Long, enabled: Boolean)
    suspend fun updateApplyInvestmentsAccountEffects(budgetId: Long, enabled: Boolean)

    /** Edits the net-worth goal shown as "Planned" on the Summary tab — always allowed, even for a completed budget. */
    suspend fun updatePlannedNetWorth(budgetId: Long, plannedNetWorth: BigDecimal)

    /** Auto-marks items whose payment day has passed as paid, for Active budgets only; call when a budget screen opens. */
    suspend fun checkAndAutoCompleteDueItems()
}

class BudgetIsCompletedException : Exception("Budget is marked as completed and can't be deleted")
