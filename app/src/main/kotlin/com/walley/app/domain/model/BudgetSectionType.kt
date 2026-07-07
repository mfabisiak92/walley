package com.walley.app.domain.model

enum class BudgetSectionType(val label: String) {
    INCOME("Income"),
    INCOME_RELATED_EXPENSES("Income-related expenses"),
    FIXED_COSTS("Fixed costs"),
    OTHER_COSTS("Other costs"),
    SAVINGS("Savings"),
    INVESTMENTS("Investments")
}

/** Sections a user can set a target % of disposable income for. */
val CATEGORY_TARGET_SECTIONS: List<BudgetSectionType> = listOf(
    BudgetSectionType.FIXED_COSTS,
    BudgetSectionType.OTHER_COSTS,
    BudgetSectionType.SAVINGS,
    BudgetSectionType.INVESTMENTS
)

/** Spending ceilings — a target here is a limit you don't want to exceed. */
val BudgetSectionType.isSpendingLimit: Boolean
    get() = this == BudgetSectionType.FIXED_COSTS || this == BudgetSectionType.OTHER_COSTS

/** Savings/investment floors — a target here is a goal you want to reach or exceed. */
val BudgetSectionType.isSavingsGoal: Boolean
    get() = this == BudgetSectionType.SAVINGS || this == BudgetSectionType.INVESTMENTS
