package com.walley.app.data.local

import com.walley.app.domain.model.FinancialSnapshot
import java.math.BigDecimal

fun FinancialSnapshotEntity.toDomain(): FinancialSnapshot = FinancialSnapshot(
    id = id,
    budgetId = budgetId,
    year = year,
    month = month,
    baseCurrency = baseCurrency,
    cashAndChecking = BigDecimal(cashAndCheckingMinorUnits).movePointLeft(2),
    savings = BigDecimal(savingsMinorUnits).movePointLeft(2),
    investments = BigDecimal(investmentsMinorUnits).movePointLeft(2),
    assets = BigDecimal(assetsMinorUnits).movePointLeft(2),
    liabilities = BigDecimal(liabilitiesMinorUnits).movePointLeft(2),
    netWorth = BigDecimal(netWorthMinorUnits).movePointLeft(2),
    income = BigDecimal(incomeMinorUnits).movePointLeft(2),
    incomeRelatedExpenses = BigDecimal(incomeRelatedExpensesMinorUnits).movePointLeft(2),
    disposableIncome = BigDecimal(disposableIncomeMinorUnits).movePointLeft(2),
    salaryIncome = BigDecimal(salaryIncomeMinorUnits).movePointLeft(2),
    dividendsIncome = BigDecimal(dividendsIncomeMinorUnits).movePointLeft(2),
    interestIncome = BigDecimal(interestIncomeMinorUnits).movePointLeft(2),
    otherIncome = BigDecimal(otherIncomeMinorUnits).movePointLeft(2),
    investmentGrowth = investmentGrowthMinorUnits?.let { BigDecimal(it).movePointLeft(2) }
)
