package com.walley.app.domain.model

import java.math.BigDecimal

data class BudgetItem(
    val id: Long = 0,
    val budgetId: Long = 0,
    val section: BudgetSectionType,
    val name: String,
    val amount: BigDecimal,
    val currency: Currency,
    /**
     * Set for [BudgetSectionType.SAVINGS] / [BudgetSectionType.INVESTMENTS] items (the account the
     * contribution lands in), and mandatory (a Checking/Cash account) for new
     * [BudgetSectionType.INCOME] / [BudgetSectionType.INCOME_RELATED_EXPENSES] items. May be null on
     * older Income/Income-related-expenses items created before that requirement existed.
     */
    val accountId: Long? = null,
    val paymentDay: Int? = null,
    val paymentDayIsLastOfMonth: Boolean = false,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    /** Only set for [BudgetSectionType.INCOME] items; used to break income down by source in the monthly snapshot. */
    val incomeCategory: IncomeCategory? = null,
    val icon: BudgetItemIcon? = null,
    /**
     * One-way lock, only settable on [BudgetSectionType.INCOME] / [BudgetSectionType.INCOME_RELATED_EXPENSES]
     * items: once true, [amount]/[paidAmount] can no longer change for this item, even if it isn't
     * fully paid. Set automatically when the item is marked fully paid, or explicitly (with
     * confirmation) to lock in a partial payment as final. See [additionalAmountToSpend] for how a
     * finalized item's deviation from plan feeds back into spendable budget elsewhere.
     */
    val isFinalized: Boolean = false
) {
    val isCompleted: Boolean get() = amount.signum() > 0 && paidAmount >= amount

    val hasPaymentDay: Boolean get() = paymentDay != null || paymentDayIsLastOfMonth
}
