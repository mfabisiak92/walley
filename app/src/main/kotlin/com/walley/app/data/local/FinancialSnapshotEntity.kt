package com.walley.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.walley.app.domain.model.Currency

@Entity(tableName = "financial_snapshots")
data class FinancialSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val budgetId: Long,
    val year: Int,
    val month: Int,
    val baseCurrency: Currency,
    val cashAndCheckingMinorUnits: Long,
    val savingsMinorUnits: Long,
    val investmentsMinorUnits: Long,
    val assetsMinorUnits: Long,
    val liabilitiesMinorUnits: Long,
    val netWorthMinorUnits: Long,
    val incomeMinorUnits: Long,
    val incomeRelatedExpensesMinorUnits: Long,
    val disposableIncomeMinorUnits: Long,
    val salaryIncomeMinorUnits: Long,
    val dividendsIncomeMinorUnits: Long,
    val interestIncomeMinorUnits: Long,
    val otherIncomeMinorUnits: Long,
    val investmentGrowthMinorUnits: Long? = null
)
