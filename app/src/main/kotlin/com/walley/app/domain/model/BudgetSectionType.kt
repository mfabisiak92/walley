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

/** Account types a user can link an item in this section to; null if accounts don't apply to this section. */
fun BudgetSectionType.allowedAccountTypes(): Set<AccountType>? = when (this) {
    BudgetSectionType.INCOME, BudgetSectionType.INCOME_RELATED_EXPENSES ->
        setOf(AccountType.CHECKING, AccountType.CASH, AccountType.INVESTMENT)
    // Deliberately excludes Investment accounts — pulling an expense straight out of an investment
    // position isn't supported, only Cash/Checking/Saving.
    BudgetSectionType.OTHER_COSTS -> setOf(AccountType.CHECKING, AccountType.CASH, AccountType.SAVING)
    BudgetSectionType.SAVINGS -> setOf(AccountType.SAVING)
    BudgetSectionType.INVESTMENTS -> setOf(AccountType.INVESTMENT)
    BudgetSectionType.FIXED_COSTS -> null
}
