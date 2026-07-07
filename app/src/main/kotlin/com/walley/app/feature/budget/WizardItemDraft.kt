package com.walley.app.feature.budget

import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.IncomeCategory
import java.math.BigDecimal

/** In-memory item held only while the create-budget wizard is in progress. */
data class WizardItemDraft(
    val localId: Long,
    val name: String,
    val amount: BigDecimal,
    val currency: Currency,
    val accountId: Long? = null,
    val paymentDay: Int? = null,
    val paymentDayIsLastOfMonth: Boolean = false,
    val incomeCategory: IncomeCategory? = null,
    val icon: BudgetItemIcon? = null
)
