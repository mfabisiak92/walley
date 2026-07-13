package com.walley.app.data.repository

import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.AdHocBudgetWithItems
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface AdHocBudgetRepository {
    fun observeAdHocBudgetsWithItems(): Flow<List<AdHocBudgetWithItems>>
    fun observeAdHocBudget(budgetId: Long): Flow<AdHocBudgetWithItems?>

    /**
     * Persists the wizard's current progress as a Draft (creating it if [budgetId] is null, replacing
     * its items otherwise). Drafts aren't shown as resolved budgets and never withdraw from the
     * account. [items]' id/budgetId are ignored.
     */
    suspend fun saveDraft(
        budgetId: Long?,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long,
        items: List<AdHocBudgetItem>
    ): Long

    /** Finalizes an ad-hoc budget (Draft -> resolved if [budgetId] is given, otherwise a brand-new one). */
    suspend fun submitBudget(
        budgetId: Long?,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long,
        items: List<AdHocBudgetItem>
    ): Long

    /** @throws InsufficientAccountBalanceException if the linked account doesn't have enough balance to cover this payment. */
    suspend fun markItemPaid(itemId: Long)

    /** @throws InsufficientAccountBalanceException if the linked account doesn't have enough balance to cover this payment. */
    suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal)

    /**
     * Edits an item's planned amount; if it drops below the amount already paid, paidAmount is clamped down to match.
     * @throws InsufficientAccountBalanceException if the linked account doesn't have enough balance to cover this payment.
     */
    suspend fun updateItemAmount(itemId: Long, amount: BigDecimal)

    suspend fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?)

    /** Deletes a single item, without reversing any account balance change it already applied. */
    suspend fun deleteBudgetItem(itemId: Long)

    /** Re-inserts a previously deleted item (used to support "undo"), preserving its original id. */
    suspend fun restoreBudgetItem(item: AdHocBudgetItem)

    /** Adds a brand-new item to an already-persisted ad-hoc budget. Returns the new item's id. */
    suspend fun addBudgetItem(budgetId: Long, name: String, amount: BigDecimal, accountId: Long?, icon: BudgetItemIcon?): Long

    /** @throws BudgetIsCompletedException if the budget is already marked completed. */
    suspend fun deleteAdHocBudget(budgetId: Long)

    /** One-way: marks the budget completed, same as [BudgetRepository.markBudgetCompleted] does for monthly budgets. */
    suspend fun markCompleted(budgetId: Long)
}

class InsufficientAccountBalanceException(
    val accountName: String,
    val available: BigDecimal,
    val requested: BigDecimal,
    val currency: Currency
) : Exception("Insufficient balance in \"$accountName\"")
