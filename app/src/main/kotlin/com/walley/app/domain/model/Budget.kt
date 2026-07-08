package com.walley.app.domain.model

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class Budget(
    val id: Long = 0,
    val year: Int,
    val month: Int,
    val status: BudgetStatus = BudgetStatus.ACTIVE,
    /** Whether paying/editing/un-paying an item in each [AccountEffectsGroup] moves money in its linked account. */
    val applyIncomeAccountEffects: Boolean = true,
    val applyCostsAccountEffects: Boolean = true,
    val applySavingsAccountEffects: Boolean = true,
    val applyInvestmentsAccountEffects: Boolean = true
) {
    val yearMonth: YearMonth get() = YearMonth.of(year, month)

    val displayName: String
        get() = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} $year"

    fun accountEffectsEnabled(group: AccountEffectsGroup): Boolean = when (group) {
        AccountEffectsGroup.INCOME -> applyIncomeAccountEffects
        AccountEffectsGroup.COSTS -> applyCostsAccountEffects
        AccountEffectsGroup.SAVINGS -> applySavingsAccountEffects
        AccountEffectsGroup.INVESTMENTS -> applyInvestmentsAccountEffects
    }
}

data class BudgetWithItems(
    val budget: Budget,
    val items: List<BudgetItem>
)
