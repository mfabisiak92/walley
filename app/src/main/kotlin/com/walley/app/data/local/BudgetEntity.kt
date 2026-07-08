package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.BudgetStatus

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val year: Int,
    val month: Int,
    val status: BudgetStatus = BudgetStatus.ACTIVE,
    val applyIncomeAccountEffects: Boolean = true,
    val applyCostsAccountEffects: Boolean = true,
    val applySavingsAccountEffects: Boolean = true,
    val applyInvestmentsAccountEffects: Boolean = true
)
