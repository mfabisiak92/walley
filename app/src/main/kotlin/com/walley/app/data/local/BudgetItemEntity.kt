package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.IncomeCategory

@Entity(tableName = "budget_items")
data class BudgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val section: BudgetSectionType,
    val name: String,
    val amountMinorUnits: Long,
    val currency: Currency,
    val accountId: Long? = null,
    val paymentDay: Int? = null,
    val paymentDayIsLastOfMonth: Boolean = false,
    val paidAmountMinorUnits: Long = 0,
    val incomeCategory: IncomeCategory? = null,
    val icon: BudgetItemIcon? = null
)
