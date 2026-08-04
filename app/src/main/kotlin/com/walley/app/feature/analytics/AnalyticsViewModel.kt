package com.walley.app.feature.analytics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.InvestmentCategoryColors
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.PieSlice
import com.walley.app.core.ui.TreemapItem
import com.walley.app.data.repository.AccountOperationRepository
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AssetRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.LiabilityRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.data.repository.SnapshotRepository
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.FinancialSnapshot
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.cagr
import com.walley.app.domain.model.displayName
import com.walley.app.domain.model.xirr
import com.walley.app.feature.budget.BudgetProgress
import com.walley.app.feature.budget.SPENDING_SECTIONS
import com.walley.app.feature.budget.budgetProgress
import com.walley.app.feature.budget.convertToCurrency
import com.walley.app.feature.budget.plannedDisposableIncome
import com.walley.app.feature.budget.sectionTotal
import com.walley.app.feature.home.calculateNetWorth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val yearMonth: YearMonth,
    val label: String,
    val income: BigDecimal?,
    val expenses: BigDecimal?,
    val savings: BigDecimal?,
    val savingsRatePercent: BigDecimal?,
    val progress: BudgetProgress?
)

data class InvestmentYearPoint(
    val year: Int,
    /** Sum of [com.walley.app.domain.model.InvestmentWithTransactions.realizedGainLossInYear] across every investment, converted to base currency. */
    val realizedGainLoss: BigDecimal,
    /** Sum of BUY transactions' net amount dated in [year] across every investment, converted to base currency. */
    val contributions: BigDecimal,
    /** Sum of positive (deposit) [com.walley.app.domain.model.AccountOperation.amount] dated in [year] across every investment account, converted to base currency. */
    val deposits: BigDecimal
)

/** [xirr]/[cagr] are each in [currency] (the position's own) — never converted, see their KDoc. */
data class InvestmentPerformancePoint(
    val name: String,
    val ticker: String,
    val currency: Currency,
    val xirr: BigDecimal?,
    val cagr: BigDecimal?
)

data class SnapshotPoint(
    val yearMonth: YearMonth,
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
    @ApplicationContext private val context: Context,
    budgetRepository: BudgetRepository,
    snapshotRepository: SnapshotRepository,
    investmentRepository: InvestmentRepository,
    accountRepository: AccountRepository,
    accountOperationRepository: AccountOperationRepository,
    assetRepository: AssetRepository,
    liabilityRepository: LiabilityRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val baseCurrencyRates = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base -> exchangeRateRepository.observeRates(base).map { rates -> base to rates } }

    val baseCurrency: StateFlow<Currency> = settingsRepository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    /** Real (non-Draft) budgets, oldest first — the shared basis for every budget-derived trend below. */
    private val nonDraftBudgetsWithItems = budgetRepository.observeBudgetsWithItems()
        .map { budgetsWithItems ->
            budgetsWithItems
                // Drafts aren't real budgets yet, so they'd only skew the trend with incomplete data.
                .filter { it.budget.status != BudgetStatus.DRAFT }
                .sortedWith(compareBy({ it.budget.year }, { it.budget.month }))
        }

    val history: StateFlow<List<BudgetHistoryPoint>> = combine(
        nonDraftBudgetsWithItems,
        baseCurrencyRates
    ) { budgetsWithItems, (base, rates) ->
        budgetsWithItems.map { toHistoryPoint(it, base, rates) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Fixed + Other costs, broken down by icon category, one point per non-Draft budget. */
    val categorySpendPoints: StateFlow<List<CategorySpendPoint>> = combine(
        nonDraftBudgetsWithItems,
        baseCurrencyRates
    ) { budgetsWithItems, (base, rates) ->
        categorySpendHistory(budgetsWithItems, base, rates, context)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** [categorySpendPoints] reduced to the top 5 categories by total spend, plus an "Other" bucket. */
    val categorySpending: StateFlow<CappedCategorySpend> = categorySpendPoints
        .map { points -> capToTopCategories(points, context) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CappedCategorySpend(emptyList(), emptyList()))

    /** Currency snapshot amounts are shown in — the most recently recorded snapshot's base currency. */
    val snapshotCurrency: StateFlow<Currency> = snapshotRepository.observeSnapshots()
        .map { it.lastOrNull()?.baseCurrency ?: Currency.PLN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    val snapshotHistory: StateFlow<List<SnapshotPoint>> = snapshotRepository.observeSnapshots()
        .map { snapshots -> snapshots.map(::toSnapshotPoint) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Month-over-month and year-over-year net worth growth rate, one point per recorded snapshot. */
    val netWorthGrowth: StateFlow<List<GrowthRatePoint>> = snapshotHistory
        .map { snapshots -> netWorthGrowthRates(snapshots) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val baseCurrencyRatesAndNetWorthSettings = combine(
        baseCurrencyRates,
        settingsRepository.observeIncludeSavingsInNetWorth()
    ) { (base, rates), includeSavings -> Triple(base, rates, includeSavings) }

    /** Actual net worth as of today (not projected end-of-month) — the "this month" side of a comparison. */
    private val liveNetWorth = combine(
        accountRepository.observeAccounts(),
        assetRepository.observeAssets(),
        liabilityRepository.observeLiabilities(),
        baseCurrencyRatesAndNetWorthSettings
    ) { accounts, assets, liabilities, (base, rates, includeSavings) ->
        calculateNetWorth(accounts, assets, liabilities, base, rates, includeSavings)
    }

    data class PeriodComparisons(
        val income: ComparisonValue,
        val expenses: ComparisonValue,
        val savings: ComparisonValue,
        val savingsRatePercent: ComparisonValue,
        val netWorth: ComparisonValue
    )

    /** This month vs. the previous calendar month. */
    val monthOverMonth: StateFlow<PeriodComparisons?> = periodComparisons { it.minusMonths(1) }

    /** This month vs. the same month one year ago. */
    val yearOverYear: StateFlow<PeriodComparisons?> = periodComparisons { it.minusYears(1) }

    private fun periodComparisons(previousPeriod: (YearMonth) -> YearMonth): StateFlow<PeriodComparisons?> = combine(
        history,
        snapshotHistory,
        liveNetWorth
    ) { historyPoints, snapshots, currentNetWorth ->
        val now = YearMonth.now()
        val previousMonth = previousPeriod(now)
        val current = findByYearMonth(historyPoints, now) { it.yearMonth }
        val previous = findByYearMonth(historyPoints, previousMonth) { it.yearMonth }
        val previousNetWorth = findByYearMonth(snapshots, previousMonth) { it.yearMonth }?.netWorth
        PeriodComparisons(
            income = compare(current?.income, previous?.income),
            expenses = compare(current?.expenses, previous?.expenses),
            savings = compare(current?.savings, previous?.savings),
            savingsRatePercent = compare(current?.savingsRatePercent, previous?.savingsRatePercent),
            netWorth = compare(currentNetWorth, previousNetWorth)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Current value of every investment, grouped by category and converted to base currency. */
    val investmentCategoryBreakdown: StateFlow<List<PieSlice>> = combine(
        investmentRepository.observeInvestments(),
        baseCurrencyRates
    ) { investments, (base, rates) ->
        val totalsByCategory = InvestmentCategory.entries.associateWith { category ->
            investments
                .filter { it.investment.category == category }
                .fold(BigDecimal.ZERO) { acc, investment ->
                    acc + (convertToCurrency(investment.currentValue, investment.investment.currency, base, rates) ?: BigDecimal.ZERO)
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
                        label = category.displayName(context),
                        value = formatMoney(value, base),
                        percent = (value.divide(total, 6, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat(),
                        color = InvestmentCategoryColors.getValue(category)
                    )
                }
                .sortedByDescending { it.percent }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every held investment position, individually, current value converted to base currency, largest first. */
    val investmentTreemap: StateFlow<List<TreemapItem>> = combine(
        investmentRepository.observeInvestments(),
        baseCurrencyRates
    ) { investments, (base, rates) ->
        investments
            .mapNotNull { data ->
                val value = convertToCurrency(data.currentValue, data.investment.currency, base, rates) ?: return@mapNotNull null
                if (value.signum() <= 0) return@mapNotNull null
                TreemapItem(
                    label = data.investment.ticker.ifBlank { data.investment.name },
                    displayValue = formatMoney(value, base),
                    value = value.toFloat(),
                    color = InvestmentCategoryColors.getValue(data.investment.category)
                )
            }
            .sortedByDescending { it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current value of every investment, grouped by its linked account (or "Unlinked") and converted to base currency. */
    val investmentAccountBreakdown: StateFlow<List<PieSlice>> = combine(
        investmentRepository.observeInvestments(),
        accountRepository.observeAccounts(),
        baseCurrencyRates
    ) { investments, accounts, (base, rates) ->
        val accountNamesById = accounts.associate { it.id to it.name }
        val unlinkedLabel = context.getString(R.string.analytics_unlinked_account)
        val totalsByAccountName = investments
            .groupBy { data -> data.investment.accountId?.let { accountNamesById[it] } ?: unlinkedLabel }
            .mapValues { (_, data) ->
                data.fold(BigDecimal.ZERO) { acc, investment ->
                    acc + (convertToCurrency(investment.currentValue, investment.investment.currency, base, rates) ?: BigDecimal.ZERO)
                }
            }
        toPieSlices(totalsByAccountName, base)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current value of every investment, grouped by its own currency and converted to base currency. */
    val investmentCurrencyBreakdown: StateFlow<List<PieSlice>> = combine(
        investmentRepository.observeInvestments(),
        baseCurrencyRates
    ) { investments, (base, rates) ->
        val totalsByCurrency = investments
            .groupBy { it.investment.currency.name }
            .mapValues { (_, data) ->
                data.fold(BigDecimal.ZERO) { acc, investment ->
                    acc + (convertToCurrency(investment.currentValue, investment.investment.currency, base, rates) ?: BigDecimal.ZERO)
                }
            }
        toPieSlices(totalsByCurrency, base)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** One point per calendar year spanned by the portfolio's transaction and deposit history, oldest first. */
    val investmentYearlyHistory: StateFlow<List<InvestmentYearPoint>> = combine(
        investmentRepository.observeInvestments(),
        accountRepository.observeAccounts(),
        accountOperationRepository.observeAll(),
        baseCurrencyRates
    ) { investments, accounts, operations, (base, rates) ->
        val currencyByAccountId = accounts.associate { it.id to it.currency }
        val transactionYears = investments.flatMap { data -> data.transactions.map { it.date.year } }
        val depositYears = operations.filter { it.amount.signum() > 0 }.map { it.date.year }
        val years = transactionYears + depositYears
        if (years.isEmpty()) {
            emptyList()
        } else {
            (years.min()..years.max()).map { year ->
                val realized = investments.fold(BigDecimal.ZERO) { acc, data ->
                    val gain = data.realizedGainLossInYear(year)
                    acc + (convertToCurrency(gain, data.investment.currency, base, rates) ?: BigDecimal.ZERO)
                }
                val contributions = investments.fold(BigDecimal.ZERO) { acc, data ->
                    val investedInYear = data.transactions
                        .filter { it.type == InvestmentTransactionType.BUY && it.date.year == year }
                        .fold(BigDecimal.ZERO) { sum, t -> sum + t.netAmount }
                    acc + (convertToCurrency(investedInYear, data.investment.currency, base, rates) ?: BigDecimal.ZERO)
                }
                val deposits = operations
                    .filter { it.amount.signum() > 0 && it.date.year == year }
                    .fold(BigDecimal.ZERO) { acc, operation ->
                        val currency = currencyByAccountId[operation.accountId] ?: base
                        acc + (convertToCurrency(operation.amount, currency, base, rates) ?: BigDecimal.ZERO)
                    }
                InvestmentYearPoint(year, realized, contributions, deposits)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Per-position XIRR/CAGR, each in that position's own currency (never converted — see [xirr]). */
    val investmentPerformance: StateFlow<List<InvestmentPerformancePoint>> = investmentRepository.observeInvestments()
        .map { investments ->
            investments.map { position ->
                InvestmentPerformancePoint(
                    name = position.investment.name,
                    ticker = position.investment.ticker,
                    currency = position.investment.currency,
                    xirr = position.xirr(),
                    cagr = position.cagr()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All-time realized + unrealized gain/loss across every position, in base currency. */
    val portfolioGainsSummary: StateFlow<PortfolioGainsSummary?> = combine(
        investmentRepository.observeInvestments(),
        baseCurrencyRates
    ) { investments, (base, rates) ->
        portfolioGainsSummary(investments, base, rates)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Shares each key's value as a percent of the total, largest first; drops zero/negative entries. */
    private fun toPieSlices(totalsByLabel: Map<String, BigDecimal>, currency: Currency): List<PieSlice> {
        val total = totalsByLabel.values.fold(BigDecimal.ZERO) { acc, value -> acc + value }
        if (total.signum() <= 0) return emptyList()
        return totalsByLabel
            .filter { (_, value) -> value.signum() > 0 }
            .entries
            .sortedByDescending { it.value }
            .mapIndexed { index, (label, value) ->
                PieSlice(
                    label = label,
                    value = formatMoney(value, currency),
                    percent = (value.divide(total, 6, RoundingMode.HALF_UP) * BigDecimal(100)).toFloat(),
                    color = PieChartColors[index % PieChartColors.size]
                )
            }
    }

    private fun toHistoryPoint(budgetWithItems: BudgetWithItems, base: Currency, rates: ExchangeRates?): BudgetHistoryPoint {
        val items = budgetWithItems.items
        val fixed = sectionTotal(items, BudgetSectionType.FIXED_COSTS, base, rates)
        val other = sectionTotal(items, BudgetSectionType.OTHER_COSTS, base, rates)
        val savings = sectionTotal(items, BudgetSectionType.SAVINGS, base, rates)
        val investments = sectionTotal(items, BudgetSectionType.INVESTMENTS, base, rates)
        val disposable = plannedDisposableIncome(items, base, rates)
        val expensesTotal = if (fixed != null && other != null) fixed + other else null
        val savingsTotal = if (savings != null && investments != null) savings + investments else null
        val savingsRate = if (disposable != null && disposable.signum() > 0 && savingsTotal != null) {
            (savingsTotal.divide(disposable, 4, RoundingMode.HALF_UP) * BigDecimal(100))
        } else {
            null
        }

        return BudgetHistoryPoint(
            yearMonth = budgetWithItems.budget.yearMonth,
            label = shortLabel(budgetWithItems.budget.yearMonth),
            income = sectionTotal(items, BudgetSectionType.INCOME, base, rates),
            expenses = expensesTotal,
            savings = savingsTotal,
            savingsRatePercent = savingsRate,
            progress = budgetProgress(items, SPENDING_SECTIONS, base, rates)
        )
    }

    private fun toSnapshotPoint(snapshot: FinancialSnapshot): SnapshotPoint = SnapshotPoint(
        yearMonth = snapshot.yearMonth,
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
