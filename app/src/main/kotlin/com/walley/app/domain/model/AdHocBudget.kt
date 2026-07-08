package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/** A one-off, date-ranged budget (e.g. "Kitchen renovation") drawn from a single Saving account. */
data class AdHocBudget(
    val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    /** The Saving account every item's cost is withdrawn from. */
    val accountId: Long,
    val currency: Currency,
    /** Whether paying/editing/un-paying an item in this budget moves money in the linked account. */
    val applyAccountEffects: Boolean = true
)

data class AdHocBudgetItem(
    val id: Long = 0,
    val budgetId: Long = 0,
    val name: String,
    val amount: BigDecimal,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val icon: BudgetItemIcon? = null
) {
    val isCompleted: Boolean get() = amount.signum() > 0 && paidAmount >= amount
}

data class AdHocBudgetWithItems(
    val budget: AdHocBudget,
    val items: List<AdHocBudgetItem>
) {
    val totalPlanned: BigDecimal get() = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
    val totalPaid: BigDecimal get() = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.paidAmount }
}
