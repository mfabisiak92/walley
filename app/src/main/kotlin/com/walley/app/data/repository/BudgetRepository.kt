package com.walley.app.data.repository

import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetWithItems
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeBudgetsWithItems(): Flow<List<BudgetWithItems>>
    fun observeBudget(budgetId: Long): Flow<BudgetWithItems?>
    suspend fun monthHasBudget(year: Int, month: Int): Boolean

    /** [items] should carry section/name/amount/currency/accountId/paymentDay; id and budgetId are ignored. */
    suspend fun createBudget(year: Int, month: Int, items: List<BudgetItem>): Long

    suspend fun markItemPaid(itemId: Long)
    suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal)

    /** @throws BudgetHasAppliedItemsException if any Savings/Investments item already affected an account balance. */
    suspend fun deleteBudget(budgetId: Long)

    /** Deletes a single item, overriding the usual locked-after-creation restriction. */
    suspend fun deleteBudgetItem(itemId: Long)

    /** Re-inserts a previously deleted item (used to support "undo"), preserving its original id. */
    suspend fun restoreBudgetItem(item: BudgetItem)

    /** Auto-marks items whose payment day has passed as paid; call when a budget screen opens. */
    suspend fun checkAndAutoCompleteDueItems()
}

class BudgetHasAppliedItemsException : Exception("Budget has completed items that already affected account balances")
