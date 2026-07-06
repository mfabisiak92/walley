package com.walley.app.domain.model

enum class BudgetSectionType(val label: String) {
    INCOME("Income"),
    INCOME_RELATED_EXPENSES("Income-related expenses"),
    FIXED_COSTS("Fixed costs"),
    OTHER_COSTS("Other costs"),
    SAVINGS("Savings"),
    INVESTMENTS("Investments")
}
