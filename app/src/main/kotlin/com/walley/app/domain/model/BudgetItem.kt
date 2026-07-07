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
    val paidAmount: BigDecimal = BigDecimal.ZERO
) {
    val isCompleted: Boolean get() = amount.signum() > 0 && paidAmount >= amount

    val hasPaymentDay: Boolean get() = paymentDay != null || paymentDayIsLastOfMonth
}
