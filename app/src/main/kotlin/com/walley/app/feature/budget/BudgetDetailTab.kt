package com.walley.app.feature.budget

import com.walley.app.domain.model.BudgetSectionType

enum class BudgetDetailTab(val label: String, val sections: Set<BudgetSectionType>) {
    SUMMARY("Summary", emptySet()),
    INCOME("Income", setOf(BudgetSectionType.INCOME, BudgetSectionType.INCOME_RELATED_EXPENSES)),
    FIXED_COSTS("Fixed costs", setOf(BudgetSectionType.FIXED_COSTS)),
    OTHER_COSTS("Other costs", setOf(BudgetSectionType.OTHER_COSTS)),
    SAVINGS("Savings", setOf(BudgetSectionType.SAVINGS)),
    INVESTMENTS("Investments", setOf(BudgetSectionType.INVESTMENTS))
}
