package com.walley.app.feature.budget

import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal

private val LEFTOVER_SECTIONS = setOf(BudgetSectionType.FIXED_COSTS, BudgetSectionType.OTHER_COSTS)

data class SurplusCategory(val label: String, val amount: BigDecimal)

data class CurrencySurplus(val currency: Currency, val total: BigDecimal, val categories: List<SurplusCategory>)

/**
 * Underspend (planned - paid) on Fixed/Other cost items, grouped by currency then by icon label —
 * money that was budgeted for a category but never actually spent. Pooled strictly per currency,
 * with no exchange-rate conversion: a leftover amount can only be allocated to a same-currency
 * savings account, never converted into another one.
 */
fun budgetSurplus(items: List<BudgetItem>, uncategorizedLabel: String): List<CurrencySurplus> {
    val byCurrency = LinkedHashMap<Currency, LinkedHashMap<String, BigDecimal>>()
    for (item in items) {
        if (item.section !in LEFTOVER_SECTIONS) continue
        val leftover = item.amount - item.paidAmount
        if (leftover.signum() <= 0) continue
        val label = item.icon?.label ?: uncategorizedLabel
        val categories = byCurrency.getOrPut(item.currency) { LinkedHashMap() }
        categories[label] = (categories[label] ?: BigDecimal.ZERO) + leftover
    }
    return byCurrency.map { (currency, categories) ->
        CurrencySurplus(
            currency = currency,
            total = categories.values.fold(BigDecimal.ZERO, BigDecimal::add),
            categories = categories.map { (label, amount) -> SurplusCategory(label, amount) }
        )
    }
}
