package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/** A one-off, date-ranged budget (e.g. "Kitchen renovation") drawn from Saving account(s). */
data class AdHocBudget(
    val id: Long = 0,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    /** The default Saving account items are withdrawn from, unless an item overrides it. */
    val accountId: Long,
    val currency: Currency,
    /** Set only by explicit user action — never derived from payment progress or [endDate] passing. */
    val isCompleted: Boolean = false,
    /** Being built in the creation wizard; not yet submitted, so it doesn't show as a resolved budget. */
    val isDraft: Boolean = false
)

data class AdHocBudgetItem(
    val id: Long = 0,
    val budgetId: Long = 0,
    val name: String,
    val amount: BigDecimal,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val icon: BudgetItemIcon? = null,
    /** Overrides [AdHocBudget.accountId] for this item; null means it draws from the budget's default account. */
    val accountId: Long? = null
) {
    val isCompleted: Boolean get() = amount.signum() > 0 && paidAmount >= amount
}

/** The account an item actually withdraws from: its own override, or the budget's default. */
fun AdHocBudgetItem.effectiveAccountId(budget: AdHocBudget): Long = accountId ?: budget.accountId

data class AdHocBudgetWithItems(
    val budget: AdHocBudget,
    val items: List<AdHocBudgetItem>
) {
    /** Only meaningful when every item shares the budget's currency — use [totalsByCurrency] otherwise. */
    val totalPlanned: BigDecimal get() = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
    val totalPaid: BigDecimal get() = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.paidAmount }

    val isCompleted: Boolean get() = budget.isCompleted

    /**
     * Groups planned/paid totals by the currency of each item's effective account, since items can
     * now draw from accounts in different currencies. [accountCurrency] resolves an account id to
     * its currency (unresolvable ids fall back to the budget's own currency).
     */
    fun totalsByCurrency(accountCurrency: (Long) -> Currency?): List<AdHocCurrencyTotal> =
        items.groupBy { accountCurrency(it.effectiveAccountId(budget)) ?: budget.currency }
            .map { (currency, groupItems) ->
                AdHocCurrencyTotal(
                    currency = currency,
                    planned = groupItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount },
                    paid = groupItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.paidAmount }
                )
            }
            .sortedByDescending { it.currency == budget.currency }
}

data class AdHocCurrencyTotal(
    val currency: Currency,
    val planned: BigDecimal,
    val paid: BigDecimal
)
