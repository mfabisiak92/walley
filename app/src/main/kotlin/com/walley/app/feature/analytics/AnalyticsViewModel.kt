package com.walley.app.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.core.ui.InvestmentCategoryColors
import com.walley.app.core.ui.PieSlice
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.data.repository.SnapshotRepository
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.FinancialSnapshot
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.feature.budget.BudgetProgress
import com.walley.app.feature.budget.SPENDING_SECTIONS
import com.walley.app.feature.budget.budgetProgress
import com.walley.app.feature.budget.convertToCurrency
import com.walley.app.feature.budget.disposableIncome
import com.walley.app.feature.budget.sectionTotal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BudgetHistoryPoint(
    val label: String,
    val income: BigDecimal?,
    val expenses: BigDecimal?,
    val savings: BigDecimal?,
    val savingsRatePercent: BigDecimal?,
    val progress: BudgetProgress?
)

data class SnapshotPoint(
    val label: String,
    val cashAndChecking: BigDecimal,
    val savings: BigDecimal,
    val investments: BigDecimal,
    val netWorth: BigDecimal,
    val salaryIncome: BigDecimal,
    val dividendsIncome: BigDecimal,
    val interestIncome: BigDecimal,
    val otherIncome: BigDecimal,
    val investmentGrowth: BigDecimal?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    budgetRepository: BudgetRepository,
    snapshotRepository: SnapshotRepository,
    investmentRepository: InvestmentRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val baseCurrencyRates = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base -> exchangeRateRepository.observeRates(base).map { rates -> base to rates } }

    val baseCurrency: StateFlow<Currency> = settingsRepository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    val history: StateFlow<List<BudgetHistoryPoint>> = combine(
        budgetRepository.observeBudgetsWithItems(),
        baseCurrencyRates
    ) { budgetsWithItems, (base, rates) ->
        budgetsWithItems
            // Drafts aren't real budgets yet, so they'd only skew the trend with incomplete data.
            .filter { it.budget.status != BudgetStatus.DRAFT }
            .sortedWith(compareBy({ it.budget.year }, { it.budget.month }))
            .map { toHistoryPoint(it, base, rates) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Currency snapshot amounts are shown in — the most recently recorded snapshot's base currency. */
    val snapshotCurrency: StateFlow<Currency> = snapshotRepository.observeSnapshots()
        .map { it.lastOrNull()?.baseCurrency ?: Currency.PLN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    val snapshotHistory: StateFlow<List<SnapshotPoint>> = snapshotRepository.observeSnapshots()
        .map { snapshots -> snapshots.map(::toSnapshotPoint) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current value of every investment, grouped by category and converted to base currency. */
    val investmentCategoryBreakdown: StateFlow<List<PieSlice>> = combine(
        investmentRepository.observeInvestments(),
        baseCurrencyRates
    ) { investments, (base, rates) ->
        val totalsByCategory = InvestmentCategory.entries.associateWith { category ->
            investments
                .filter { it.category == category }
                .fold(BigDecimal.ZERO) { acc, investment ->
                    acc + (convertToCurrency(investment.currentValue, investment.currency, base, rates) ?: BigDecimal.ZERO)
                }
        }
        val total = totalsByCategory.values.fold(BigDecimal.ZERO) { acc, value -> acc + value }
        if (total.signum() <= 0) {
            emptyList()
        } else {
            totalsByCategory
                .filter { (_, value) -> value.signum() > 0 }
                .map { (category, value) ->
                    PieSlice(
                        label = category.label,
                        percent = (value.divide(total, 6, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat(),
                        color = InvestmentCategoryColors.getValue(category)
                    )
                }
                .sortedByDescending { it.percent }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun toHistoryPoint(budgetWithItems: BudgetWithItems, base: Currency, rates: ExchangeRates?): BudgetHistoryPoint {
        val items = budgetWithItems.items
        val fixed = sectionTotal(items, BudgetSectionType.FIXED_COSTS, base, rates)
        val other = sectionTotal(items, BudgetSectionType.OTHER_COSTS, base, rates)
        val savings = sectionTotal(items, BudgetSectionType.SAVINGS, base, rates)
        val investments = sectionTotal(items, BudgetSectionType.INVESTMENTS, base, rates)
        val disposable = disposableIncome(items, base, rates)
        val expensesTotal = if (fixed != null && other != null) fixed + other else null
        val savingsTotal = if (savings != null && investments != null) savings + investments else null
        val savingsRate = if (disposable != null && disposable.signum() > 0 && savingsTotal != null) {
            (savingsTotal.divide(disposable, 4, RoundingMode.HALF_UP) * BigDecimal(100))
        } else {
            null
        }

        return BudgetHistoryPoint(
            label = shortLabel(budgetWithItems.budget.yearMonth),
            income = sectionTotal(items, BudgetSectionType.INCOME, base, rates),
            expenses = expensesTotal,
            savings = savingsTotal,
            savingsRatePercent = savingsRate,
            progress = budgetProgress(items, SPENDING_SECTIONS, base, rates)
        )
    }

    private fun toSnapshotPoint(snapshot: FinancialSnapshot): SnapshotPoint = SnapshotPoint(
        label = shortLabel(snapshot.yearMonth),
        cashAndChecking = snapshot.cashAndChecking,
        savings = snapshot.savings,
        investments = snapshot.investments,
        netWorth = snapshot.netWorth,
        salaryIncome = snapshot.salaryIncome,
        dividendsIncome = snapshot.dividendsIncome,
        interestIncome = snapshot.interestIncome,
        otherIncome = snapshot.otherIncome,
        investmentGrowth = snapshot.investmentGrowth
    )

    private fun shortLabel(yearMonth: YearMonth): String {
        val monthLabel = yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        val shortYear = (yearMonth.year % 100).toString().padStart(2, '0')
        return "$monthLabel '$shortYear"
    }
}
