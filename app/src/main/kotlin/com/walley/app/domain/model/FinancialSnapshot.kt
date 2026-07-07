package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.YearMonth

/**
 * A point-in-time record of key financial figures, captured automatically when a budget is
 * marked completed. All amounts are in [baseCurrency] (the Settings base currency at capture time).
 */
data class FinancialSnapshot(
    val id: Long = 0,
    val budgetId: Long,
    val year: Int,
    val month: Int,
    val baseCurrency: Currency,
    val cashAndChecking: BigDecimal,
    val savings: BigDecimal,
    val investments: BigDecimal,
    val assets: BigDecimal,
    val liabilities: BigDecimal,
    val netWorth: BigDecimal,
    val income: BigDecimal,
    val incomeRelatedExpenses: BigDecimal,
    val disposableIncome: BigDecimal,
    val salaryIncome: BigDecimal,
    val dividendsIncome: BigDecimal,
    val interestIncome: BigDecimal,
    val otherIncome: BigDecimal,
    /** Investment value change this month, net of new contributions; null when there's no prior month to diff against. */
    val investmentGrowth: BigDecimal?
) {
    val yearMonth: YearMonth get() = YearMonth.of(year, month)
}
