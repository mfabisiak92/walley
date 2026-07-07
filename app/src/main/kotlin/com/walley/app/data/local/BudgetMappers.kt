package com.walley.app.data.local

import com.walley.app.domain.model.Budget
import com.walley.app.domain.model.BudgetItem
import java.math.BigDecimal

fun BudgetEntity.toDomain(): Budget = Budget(id = id, year = year, month = month, status = status)

fun BudgetItemEntity.toDomain(): BudgetItem = BudgetItem(
    id = id,
    budgetId = budgetId,
    section = section,
    name = name,
    amount = BigDecimal(amountMinorUnits).movePointLeft(2),
    currency = currency,
    accountId = accountId,
    paymentDay = paymentDay,
    paymentDayIsLastOfMonth = paymentDayIsLastOfMonth,
    paidAmount = BigDecimal(paidAmountMinorUnits).movePointLeft(2),
    incomeCategory = incomeCategory
)
