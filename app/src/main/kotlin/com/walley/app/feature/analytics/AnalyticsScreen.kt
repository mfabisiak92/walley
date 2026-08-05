package com.walley.app.feature.analytics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.ChartSeries
import com.walley.app.core.ui.PieChartCard
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.StackedTrendChartCard
import com.walley.app.core.ui.SwipeableTrendChartCard
import com.walley.app.core.ui.TreemapChartCard
import com.walley.app.core.ui.TrendChartCard
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import kotlinx.coroutines.launch

private enum class HistoryHorizon(val months: Int?) {
    SIX_MONTHS(6),
    ONE_YEAR(12),
    TWO_YEARS(24),
    FIVE_YEARS(60),
    ALL(null)
}

private fun <T> HistoryHorizon.applyTo(items: List<T>): List<T> = months?.let { items.takeLast(it) } ?: items

@Composable
private fun HistoryHorizon.displayName(): String = when (this) {
    HistoryHorizon.SIX_MONTHS -> stringResource(R.string.analytics_horizon_6m)
    HistoryHorizon.ONE_YEAR -> stringResource(R.string.analytics_horizon_1y)
    HistoryHorizon.TWO_YEARS -> stringResource(R.string.analytics_horizon_2y)
    HistoryHorizon.FIVE_YEARS -> stringResource(R.string.analytics_horizon_5y)
    HistoryHorizon.ALL -> stringResource(R.string.analytics_horizon_all)
}

private const val TAB_COUNT = 3

private val GainColor = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { TAB_COUNT })
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        stringResource(R.string.analytics_tab_budget),
        stringResource(R.string.analytics_tab_history),
        stringResource(R.string.analytics_label_investments)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.analytics_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.analytics_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(label) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> BudgetHistoryPage(viewModel)
                    1 -> SnapshotHistoryPage(viewModel)
                    else -> InvestmentsBreakdownPage(viewModel)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Insights,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetHistoryPage(viewModel: AnalyticsViewModel) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val categorySpendPoints by viewModel.categorySpendPoints.collectAsStateWithLifecycle()
    val categorySpending by viewModel.categorySpending.collectAsStateWithLifecycle()
    val monthOverMonth by viewModel.monthOverMonth.collectAsStateWithLifecycle()
    val yearOverYear by viewModel.yearOverYear.collectAsStateWithLifecycle()

    if (history.isEmpty()) {
        EmptyState(stringResource(R.string.analytics_empty_no_budgets))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val labels = history.map { it.label }
        val moneyFormatter = { value: Float -> formatMoney(BigDecimal.valueOf(value.toDouble()), baseCurrency) }

        val incomeLabel = stringResource(R.string.analytics_label_income)
        val expensesLabel = stringResource(R.string.analytics_label_expenses)
        val savingsLabel = stringResource(R.string.analytics_label_savings)
        val savingsRateLabel = stringResource(R.string.analytics_savings_rate)
        val spentLabel = stringResource(R.string.analytics_label_spent)

        PeriodComparisonSection(monthOverMonth, yearOverYear, baseCurrency)

        TrendChartCard(
            title = stringResource(R.string.analytics_chart_income_vs_expenses_vs_savings),
            labels = labels,
            series = listOf(
                ChartSeries(incomeLabel, PieChartColors[0], history.map { it.income?.toFloat() }),
                ChartSeries(expensesLabel, PieChartColors[3], history.map { it.expenses?.toFloat() }),
                ChartSeries(savingsLabel, PieChartColors[5], history.map { it.savings?.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        TrendChartCard(
            title = savingsRateLabel,
            labels = labels,
            series = listOf(
                ChartSeries(savingsRateLabel, PieChartColors[5], history.map { it.savingsRatePercent?.toFloat() })
            ),
            valueFormatter = { value -> "${value.toInt()}%" }
        )

        TrendChartCard(
            title = stringResource(R.string.analytics_chart_budget_adherence),
            labels = labels,
            series = listOf(
                ChartSeries(spentLabel, PieChartColors[2], history.map { it.progress?.percent?.toFloat() })
            ),
            valueFormatter = { value -> "${value.toInt()}%" }
        )

        if (categorySpending.categoryLabels.isNotEmpty()) {
            StackedTrendChartCard(
                title = stringResource(R.string.analytics_chart_spending_by_category),
                labels = categorySpendPoints.map { it.label },
                series = categorySpending.categoryLabels.mapIndexed { index, label ->
                    ChartSeries(label, PieChartColors[index % PieChartColors.size], categorySpending.seriesByCategory[index])
                },
                valueFormatter = moneyFormatter
            )

            CategoryVarianceSection(categorySpendPoints, moneyFormatter)
        }
    }
}

@Composable
private fun CategoryVarianceSection(points: List<CategorySpendPoint>, moneyFormatter: (Float) -> String) {
    val categories = remember(points) { distinctCategories(points) }
    if (categories.isEmpty()) return

    var selected by remember(categories) { mutableStateOf(categories.first()) }
    val variance = remember(points, selected) { varianceForCategory(points, selected) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selected == category,
                    onClick = { selected = category },
                    label = { Text(category) }
                )
            }
        }

        TrendChartCard(
            title = stringResource(R.string.analytics_planned_vs_actual, selected),
            labels = variance.monthLabels,
            series = listOf(
                ChartSeries(stringResource(R.string.analytics_label_planned), PieChartColors[0], variance.planned),
                ChartSeries(stringResource(R.string.analytics_label_actual), PieChartColors[3], variance.actual)
            ),
            valueFormatter = moneyFormatter
        )
    }
}

private enum class ComparisonPeriod {
    LAST_MONTH,
    LAST_YEAR
}

@Composable
private fun ComparisonPeriod.displayName(): String = when (this) {
    ComparisonPeriod.LAST_MONTH -> stringResource(R.string.analytics_period_last_month)
    ComparisonPeriod.LAST_YEAR -> stringResource(R.string.analytics_period_last_year)
}

@Composable
private fun PeriodComparisonSection(
    monthOverMonth: AnalyticsViewModel.PeriodComparisons?,
    yearOverYear: AnalyticsViewModel.PeriodComparisons?,
    baseCurrency: Currency
) {
    if (monthOverMonth == null && yearOverYear == null) return

    var period by remember { mutableStateOf(ComparisonPeriod.LAST_MONTH) }
    val comparisons = if (period == ComparisonPeriod.LAST_MONTH) monthOverMonth else yearOverYear
    val moneyFormatter = { value: BigDecimal -> formatMoney(value, baseCurrency) }
    val percentFormatter = { value: BigDecimal -> "${value.toInt()}%" }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComparisonPeriod.entries.forEach { p ->
                FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.displayName()) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComparisonStatCard(stringResource(R.string.analytics_label_income), comparisons?.income, moneyFormatter, Modifier.weight(1f))
            ComparisonStatCard(stringResource(R.string.analytics_label_expenses), comparisons?.expenses, moneyFormatter, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComparisonStatCard(stringResource(R.string.analytics_label_savings), comparisons?.savings, moneyFormatter, Modifier.weight(1f))
            ComparisonStatCard(stringResource(R.string.analytics_savings_rate), comparisons?.savingsRatePercent, percentFormatter, Modifier.weight(1f))
        }
    }
}

/** A compact stat card: current value plus a colored delta arrow vs. the comparison period, "—" when unavailable. */
@Composable
private fun ComparisonStatCard(
    label: String,
    value: ComparisonValue?,
    valueFormatter: (BigDecimal) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value?.current?.let(valueFormatter) ?: "—",
                style = MaterialTheme.typography.titleMedium
            )
            val change = value?.changePercent
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (change != null) {
                    val isUp = change.signum() >= 0
                    val color = if (isUp) GainColor else MaterialTheme.colorScheme.error
                    Icon(
                        if (isUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Text("${change.abs().toInt()}%", style = MaterialTheme.typography.labelSmall, color = color)
                } else {
                    Text("—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NetWorthComparisonCard(
    monthOverMonth: AnalyticsViewModel.PeriodComparisons?,
    yearOverYear: AnalyticsViewModel.PeriodComparisons?,
    currency: Currency
) {
    if (monthOverMonth == null && yearOverYear == null) return

    var period by remember { mutableStateOf(ComparisonPeriod.LAST_MONTH) }
    val comparisons = if (period == ComparisonPeriod.LAST_MONTH) monthOverMonth else yearOverYear

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComparisonPeriod.entries.forEach { p ->
                FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.displayName()) })
            }
        }
        ComparisonStatCard(stringResource(R.string.analytics_label_net_worth), comparisons?.netWorth, { value -> formatMoney(value, currency) })
    }
}

@Composable
private fun SnapshotHistoryPage(viewModel: AnalyticsViewModel) {
    val snapshots by viewModel.snapshotHistory.collectAsStateWithLifecycle()
    val currency by viewModel.snapshotCurrency.collectAsStateWithLifecycle()
    val netWorthGrowth by viewModel.netWorthGrowth.collectAsStateWithLifecycle()
    val monthOverMonth by viewModel.monthOverMonth.collectAsStateWithLifecycle()
    val yearOverYear by viewModel.yearOverYear.collectAsStateWithLifecycle()
    var horizon by remember { mutableStateOf(HistoryHorizon.ONE_YEAR) }

    if (snapshots.isEmpty()) {
        EmptyState(stringResource(R.string.analytics_empty_no_history))
        return
    }

    val visible = remember(snapshots, horizon) { horizon.applyTo(snapshots) }
    val visibleGrowth = remember(netWorthGrowth, horizon) { horizon.applyTo(netWorthGrowth) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HistoryHorizonSelector(selected = horizon, onSelect = { horizon = it })

        val labels = visible.map { it.label }
        val moneyFormatter = { value: Float -> formatMoney(BigDecimal.valueOf(value.toDouble()), currency) }
        val netWorthLabel = stringResource(R.string.analytics_label_net_worth)

        SwipeableTrendChartCard(
            title = stringResource(R.string.analytics_chart_account_balances),
            labels = labels,
            series = listOf(
                ChartSeries(stringResource(R.string.analytics_label_cash_and_checking), PieChartColors[0], visible.map { it.cashAndChecking.toFloat() }),
                ChartSeries(stringResource(R.string.analytics_label_savings), PieChartColors[1], visible.map { it.savings.toFloat() }),
                ChartSeries(stringResource(R.string.analytics_label_investments), PieChartColors[2], visible.map { it.investments.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        NetWorthComparisonCard(monthOverMonth, yearOverYear, currency)

        SwipeableTrendChartCard(
            title = netWorthLabel,
            labels = labels,
            series = listOf(
                ChartSeries(netWorthLabel, PieChartColors[4], visible.map { it.netWorth.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        if (visibleGrowth.isNotEmpty()) {
            SwipeableTrendChartCard(
                title = stringResource(R.string.analytics_chart_net_worth_growth_rate),
                labels = visibleGrowth.map { it.label },
                series = listOf(
                    ChartSeries(stringResource(R.string.analytics_label_mom_percent), PieChartColors[2], visibleGrowth.map { it.momPercent?.toFloat() }),
                    ChartSeries(stringResource(R.string.analytics_label_yoy_percent), PieChartColors[4], visibleGrowth.map { it.yoyPercent?.toFloat() })
                ),
                valueFormatter = { value -> "${value.toInt()}%" }
            )
        }

        SwipeableTrendChartCard(
            title = stringResource(R.string.analytics_chart_income_by_source),
            labels = labels,
            series = listOf(
                ChartSeries(stringResource(R.string.analytics_label_salary), PieChartColors[0], visible.map { it.salaryIncome.toFloat() }),
                ChartSeries(stringResource(R.string.analytics_label_dividends), PieChartColors[1], visible.map { it.dividendsIncome.toFloat() }),
                ChartSeries(stringResource(R.string.analytics_label_interest), PieChartColors[3], visible.map { it.interestIncome.toFloat() }),
                ChartSeries(stringResource(R.string.analytics_label_other), PieChartColors[5], visible.map { it.otherIncome.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        SwipeableTrendChartCard(
            title = stringResource(R.string.analytics_chart_investment_growth),
            labels = labels,
            series = listOf(
                ChartSeries(stringResource(R.string.analytics_label_growth), PieChartColors[5], visible.map { it.investmentGrowth?.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )
    }
}

@Composable
private fun InvestmentsBreakdownPage(viewModel: AnalyticsViewModel) {
    val categoryBreakdown by viewModel.investmentCategoryBreakdown.collectAsStateWithLifecycle()
    val accountBreakdown by viewModel.investmentAccountBreakdown.collectAsStateWithLifecycle()
    val currencyBreakdown by viewModel.investmentCurrencyBreakdown.collectAsStateWithLifecycle()
    val yearlyHistory by viewModel.investmentYearlyHistory.collectAsStateWithLifecycle()
    val treemap by viewModel.investmentTreemap.collectAsStateWithLifecycle()
    val performance by viewModel.investmentPerformance.collectAsStateWithLifecycle()
    val gainsSummary by viewModel.portfolioGainsSummary.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()

    if (categoryBreakdown.isEmpty() && accountBreakdown.isEmpty() && currencyBreakdown.isEmpty() && yearlyHistory.isEmpty()) {
        EmptyState(stringResource(R.string.analytics_empty_no_investments))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (treemap.isNotEmpty()) {
            TreemapChartCard(title = stringResource(R.string.analytics_chart_investments_by_size), items = treemap)
        }

        PieChartCard(title = stringResource(R.string.analytics_chart_investments_by_category), slices = categoryBreakdown)
        PieChartCard(title = stringResource(R.string.analytics_chart_investments_by_account), slices = accountBreakdown)
        PieChartCard(title = stringResource(R.string.analytics_chart_investments_by_currency), slices = currencyBreakdown)

        if (yearlyHistory.isNotEmpty()) {
            val labels = yearlyHistory.map { it.year.toString() }
            val moneyFormatter = { value: Float -> formatMoney(BigDecimal.valueOf(value.toDouble()), baseCurrency) }

            SwipeableTrendChartCard(
                title = stringResource(R.string.analytics_chart_realized_gains_by_year),
                labels = labels,
                series = listOf(
                    ChartSeries(stringResource(R.string.analytics_label_realized), PieChartColors[3], yearlyHistory.map { it.realizedGainLoss.toFloat() })
                ),
                valueFormatter = moneyFormatter
            )

            TrendChartCard(
                title = stringResource(R.string.analytics_chart_contributions_by_year),
                labels = labels,
                series = listOf(
                    ChartSeries(stringResource(R.string.analytics_label_invested), PieChartColors[2], yearlyHistory.map { it.contributions.toFloat() }),
                    ChartSeries(stringResource(R.string.analytics_label_deposited), PieChartColors[4], yearlyHistory.map { it.deposits.toFloat() }),
                    ChartSeries(stringResource(R.string.analytics_label_gain_loss), PieChartColors[0], yearlyHistory.map { it.growth.toFloat() })
                ),
                valueFormatter = moneyFormatter
            )
        }

        if (gainsSummary != null) {
            AllTimeGainsCard(gainsSummary!!, baseCurrency)
        }

        if (performance.isNotEmpty()) {
            PerformanceByPositionCard(performance)
        }
    }
}

@Composable
private fun AllTimeGainsCard(summary: PortfolioGainsSummary, baseCurrency: Currency) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.analytics_all_time_gains), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GainStat(stringResource(R.string.analytics_label_realized), summary.realizedGainLoss, baseCurrency)
                GainStat(stringResource(R.string.analytics_label_unrealized), summary.unrealizedGainLoss, baseCurrency)
            }
        }
    }
}

@Composable
private fun GainStat(label: String, amount: BigDecimal, currency: Currency) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatMoney(amount, currency),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                amount.signum() > 0 -> GainColor
                amount.signum() < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun PerformanceByPositionCard(performance: List<InvestmentPerformancePoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.analytics_performance_by_position), style = MaterialTheme.typography.titleMedium)
            performance.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(point.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            point.ticker,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.analytics_xirr_value, point.xirr?.let { "${it.toInt()}%" } ?: "—"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.analytics_cagr_value, point.cagr?.let { "${it.toInt()}%" } ?: "—", point.currency.name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHorizonSelector(selected: HistoryHorizon, onSelect: (HistoryHorizon) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryHorizon.entries.forEach { horizon ->
            FilterChip(
                selected = selected == horizon,
                onClick = { onSelect(horizon) },
                label = { Text(horizon.displayName()) }
            )
        }
    }
}
