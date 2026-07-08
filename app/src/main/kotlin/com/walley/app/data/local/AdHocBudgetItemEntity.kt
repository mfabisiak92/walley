package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.BudgetItemIcon

@Entity(tableName = "adhoc_budget_items")
data class AdHocBudgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val name: String,
    val amountMinorUnits: Long,
    val paidAmountMinorUnits: Long = 0,
    val icon: BudgetItemIcon? = null
)
