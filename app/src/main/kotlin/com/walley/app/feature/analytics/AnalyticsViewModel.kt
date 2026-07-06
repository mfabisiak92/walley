package com.walley.app.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.feature.budget.BudgetProgress
import com.walley.app.feature.budget.SPENDING_SECTIONS
import com.walley.app.feature.budget.budgetProgress
import com.walley.app.feature.budget.disposableIncomePln
import com.walley.app.feature.budget.sectionTotalPln
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class BudgetHistoryPoint(
    val label: String,
    val incomePln: BigDecimal?,
    val expensesPln: BigDecimal?,
    val savingsPln: BigDecimal?,
    val savingsRatePercent: BigDecimal?,
    val progress: BudgetProgress?
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    budgetRepository: BudgetRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val plnRates = exchangeRateRepository.observeRates(Currency.PLN)

    val history: StateFlow<List<BudgetHistoryPoint>> = combine(
        budgetRepository.observeBudgetsWithItems(),
        plnRates
    ) { budgetsWithItems, plnRates ->
        budgetsWithItems
            .sortedWith(compareBy({ it.budget.year }, { it.budget.month }))
            .map { toHistoryPoint(it, plnRates) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun toHistoryPoint(budgetWithItems: BudgetWithItems, plnRates: ExchangeRates?): BudgetHistoryPoint {
        val items = budgetWithItems.items
        val fixed = sectionTotalPln(items, BudgetSectionType.FIXED_COSTS, plnRates)
        val other = sectionTotalPln(items, BudgetSectionType.OTHER_COSTS, plnRates)
        val savings = sectionTotalPln(items, BudgetSectionType.SAVINGS, plnRates)
        val investments = sectionTotalPln(items, BudgetSectionType.INVESTMENTS, plnRates)
        val disposable = disposableIncomePln(items, plnRates)
        val expensesTotal = if (fixed != null && other != null) fixed + other else null
        val savingsTotal = if (savings != null && investments != null) savings + investments else null
        val savingsRate = if (disposable != null && disposable.signum() > 0 && savingsTotal != null) {
            (savingsTotal.divide(disposable, 4, RoundingMode.HALF_UP) * BigDecimal(100))
        } else {
            null
        }

        return BudgetHistoryPoint(
            label = shortLabel(budgetWithItems),
            incomePln = sectionTotalPln(items, BudgetSectionType.INCOME, plnRates),
            expensesPln = expensesTotal,
            savingsPln = savingsTotal,
            savingsRatePercent = savingsRate,
            progress = budgetProgress(items, SPENDING_SECTIONS, plnRates)
        )
    }

    private fun shortLabel(budgetWithItems: BudgetWithItems): String {
        val yearMonth = budgetWithItems.budget.yearMonth
        val monthLabel = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        val shortYear = (yearMonth.year % 100).toString().padStart(2, '0')
        return "$monthLabel '$shortYear"
    }
}
