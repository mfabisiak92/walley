package com.walley.app.domain.model

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class Budget(
    val id: Long = 0,
    val year: Int,
    val month: Int,
    val status: BudgetStatus = BudgetStatus.ACTIVE,
    /** Whether paying/editing/un-paying an item in this budget moves money in its linked account. */
    val applyAccountEffects: Boolean = true
) {
    val yearMonth: YearMonth get() = YearMonth.of(year, month)

    val displayName: String
        get() = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} $year"
}

data class BudgetWithItems(
    val budget: Budget,
    val items: List<BudgetItem>
)
