package com.walley.app.domain.model

import java.math.BigDecimal

data class BudgetItem(
    val id: Long = 0,
    val budgetId: Long = 0,
    val section: BudgetSectionType,
    val name: String,
    val amount: BigDecimal,
    val currency: Currency,
    /** Only set for [BudgetSectionType.SAVINGS] / [BudgetSectionType.INVESTMENTS] items. */
    val accountId: Long? = null,
    val paymentDay: Int? = null,
    val paymentDayIsLastOfMonth: Boolean = false,
    val paidAmount: BigDecimal = BigDecimal.ZERO
) {
    val isCompleted: Boolean get() = amount.signum() > 0 && paidAmount >= amount

    val hasPaymentDay: Boolean get() = paymentDay != null || paymentDayIsLastOfMonth
}
