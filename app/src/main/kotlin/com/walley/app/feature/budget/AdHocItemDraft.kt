package com.walley.app.feature.budget

import com.walley.app.domain.model.BudgetItemIcon
import java.math.BigDecimal

/** An item being staged in the Ad-hoc wizard, before the budget (and its account) exists yet. */
data class AdHocItemDraft(
    val localId: Long,
    val name: String,
    val amount: BigDecimal,
    val icon: BudgetItemIcon? = null
)
