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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.ChartSeries
import com.walley.app.core.ui.PieChartCard
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.StackedTrendChartCard
import com.walley.app.core.ui.SwipeableTrendChartCard
import com.walley.app.core.ui.TrendChartCard
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import kotlinx.coroutines.launch

private enum class HistoryHorizon(val label: String, val months: Int?) {
    SIX_MONTHS("6M", 6),
    ONE_YEAR("1Y", 12),
    TWO_YEARS("2Y", 24),
    FIVE_YEARS("5Y", 60),
    ALL("∞", null)
}

private fun <T> HistoryHorizon.applyTo(items: List<T>): List<T> = months?.let { items.takeLast(it) } ?: items

private val TABS = listOf("Budget", "History", "Investments")

private val GainColor = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                TABS.forEachIndexed { index, label ->
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
        EmptyState("No budgets yet — create one to see analytics.")
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

        PeriodComparisonSection(monthOverMonth, yearOverYear, baseCurrency)

        TrendChartCard(
            title = "Income vs Expenses vs Savings",
            labels = labels,
            series = listOf(
                ChartSeries("Income", PieChartColors[0], history.map { it.income?.toFloat() }),
                ChartSeries("Expenses", PieChartColors[3], history.map { it.expenses?.toFloat() }),
                ChartSeries("Savings", PieChartColors[5], history.map { it.savings?.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        TrendChartCard(
            title = "Savings rate",
            labels = labels,
            series = listOf(
                ChartSeries("Savings rate", PieChartColors[5], history.map { it.savingsRatePercent?.toFloat() })
            ),
            valueFormatter = { value -> "${value.toInt()}%" }
        )

        TrendChartCard(
            title = "Budget adherence (spent vs planned)",
            labels = labels,
            series = listOf(
                ChartSeries("Spent", PieChartColors[2], history.map { it.progress?.percent?.toFloat() })
            ),
            valueFormatter = { value -> "${value.toInt()}%" }
        )

        if (categorySpending.categoryLabels.isNotEmpty()) {
            StackedTrendChartCard(
                title = "Spending by category",
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
            title = "Planned vs actual — $selected",
            labels = variance.monthLabels,
            series = listOf(
                ChartSeries("Planned", PieChartColors[0], variance.planned),
                ChartSeries("Actual", PieChartColors[3], variance.actual)
            ),
            valueFormatter = moneyFormatter
        )
    }
}

private enum class ComparisonPeriod(val label: String) {
    LAST_MONTH("vs last month"),
    LAST_YEAR("vs last year")
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
                FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComparisonStatCard("Income", comparisons?.income, moneyFormatter, Modifier.weight(1f))
            ComparisonStatCard("Expenses", comparisons?.expenses, moneyFormatter, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComparisonStatCard("Savings", comparisons?.savings, moneyFormatter, Modifier.weight(1f))
            ComparisonStatCard("Savings rate", comparisons?.savingsRatePercent, percentFormatter, Modifier.weight(1f))
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
                FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
            }
        }
        ComparisonStatCard("Net worth", comparisons?.netWorth, { value -> formatMoney(value, currency) })
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
        EmptyState("No history yet — mark a budget as completed to record your first snapshot.")
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

        SwipeableTrendChartCard(
            title = "Account balances",
            labels = labels,
            series = listOf(
                ChartSeries("Cash & Checking", PieChartColors[0], visible.map { it.cashAndChecking.toFloat() }),
                ChartSeries("Savings", PieChartColors[1], visible.map { it.savings.toFloat() }),
                ChartSeries("Investments", PieChartColors[2], visible.map { it.investments.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        NetWorthComparisonCard(monthOverMonth, yearOverYear, currency)

        SwipeableTrendChartCard(
            title = "Net worth",
            labels = labels,
            series = listOf(
                ChartSeries("Net worth", PieChartColors[4], visible.map { it.netWorth.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        if (visibleGrowth.isNotEmpty()) {
            SwipeableTrendChartCard(
                title = "Net worth growth rate",
                labels = visibleGrowth.map { it.label },
                series = listOf(
                    ChartSeries("MoM %", PieChartColors[2], visibleGrowth.map { it.momPercent?.toFloat() }),
                    ChartSeries("YoY %", PieChartColors[4], visibleGrowth.map { it.yoyPercent?.toFloat() })
                ),
                valueFormatter = { value -> "${value.toInt()}%" }
            )
        }

        SwipeableTrendChartCard(
            title = "Income by source",
            labels = labels,
            series = listOf(
                ChartSeries("Salary", PieChartColors[0], visible.map { it.salaryIncome.toFloat() }),
                ChartSeries("Dividends", PieChartColors[1], visible.map { it.dividendsIncome.toFloat() }),
                ChartSeries("Interest", PieChartColors[3], visible.map { it.interestIncome.toFloat() }),
                ChartSeries("Other", PieChartColors[5], visible.map { it.otherIncome.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        SwipeableTrendChartCard(
            title = "Investment growth (net of contributions)",
            labels = labels,
            series = listOf(
                ChartSeries("Growth", PieChartColors[5], visible.map { it.investmentGrowth?.toFloat() })
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
    val performance by viewModel.investmentPerformance.collectAsStateWithLifecycle()
    val gainsSummary by viewModel.portfolioGainsSummary.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()

    if (categoryBreakdown.isEmpty() && accountBreakdown.isEmpty() && currencyBreakdown.isEmpty() && yearlyHistory.isEmpty()) {
        EmptyState("No investments yet — add one from the Investments tab to see a breakdown.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PieChartCard(title = "Investments by category", slices = categoryBreakdown)
        PieChartCard(title = "Investments by account", slices = accountBreakdown)
        PieChartCard(title = "Investments by currency", slices = currencyBreakdown)

        if (yearlyHistory.isNotEmpty()) {
            val labels = yearlyHistory.map { it.year.toString() }
            val moneyFormatter = { value: Float -> formatMoney(BigDecimal.valueOf(value.toDouble()), baseCurrency) }

            SwipeableTrendChartCard(
                title = "Realized gains/losses by year",
                labels = labels,
                series = listOf(
                    ChartSeries("Realized", PieChartColors[3], yearlyHistory.map { it.realizedGainLoss.toFloat() })
                ),
                valueFormatter = moneyFormatter
            )

            TrendChartCard(
                title = "Contributions by year",
                labels = labels,
                series = listOf(
                    ChartSeries("Invested", PieChartColors[2], yearlyHistory.map { it.contributions.toFloat() })
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
            Text("All-time gains", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GainStat("Realized", summary.realizedGainLoss, baseCurrency)
                GainStat("Unrealized", summary.unrealizedGainLoss, baseCurrency)
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
            Text("Performance by position", style = MaterialTheme.typography.titleMedium)
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
                            "XIRR ${point.xirr?.let { "${it.toInt()}%" } ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "CAGR ${point.cagr?.let { "${it.toInt()}%" } ?: "—"} (${point.currency.name})",
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
                label = { Text(horizon.label) }
            )
        }
    }
}
