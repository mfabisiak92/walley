package com.walley.app.data.local

import com.walley.app.domain.model.AdHocBudget
import com.walley.app.domain.model.AdHocBudgetItem
import java.math.BigDecimal

fun AdHocBudgetEntity.toDomain(): AdHocBudget = AdHocBudget(
    id = id,
    name = name,
    startDate = startDate,
    endDate = endDate,
    accountId = accountId,
    currency = currency,
    isCompleted = isCompleted
)

fun AdHocBudgetItemEntity.toDomain(): AdHocBudgetItem = AdHocBudgetItem(
    id = id,
    budgetId = budgetId,
    name = name,
    amount = BigDecimal(amountMinorUnits).movePointLeft(2),
    paidAmount = BigDecimal(paidAmountMinorUnits).movePointLeft(2),
    icon = icon
)
