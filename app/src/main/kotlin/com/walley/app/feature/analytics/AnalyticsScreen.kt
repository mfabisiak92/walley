package com.walley.app.feature.analytics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Insights
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.ChartSeries
import com.walley.app.core.ui.PieChartCard
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.SwipeableTrendChartCard
import com.walley.app.core.ui.TrendChartCard
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

        TrendChartCard(
            title = "Income vs Expenses vs Savings",
            labels = labels,
            series = listOf(
                ChartSeries("Income", PieChartColors[0], history.map { it.income?.toFloat() }),
                ChartSeries("Expenses", PieChartColors[3], history.map { it.expenses?.toFloat() }),
                ChartSeries("Savings", PieChartColors[5], history.map { it.savings?.toFloat() })
            ),
            valueFormatter = { value -> formatMoney(BigDecimal.valueOf(value.toDouble()), baseCurrency) }
        )

        TrendChartCard(
            title = "Savings rate",
            labels = labels,
            series = listOf(
                ChartSeries("Savings rate", PieChartColors[5], history.map { it.savingsRatePercent?.toFloat() })
            ),
            valueFormatter = { value -> "${value.toInt()}%" },
            showValueLabels = true
        )

        TrendChartCard(
            title = "Budget adherence (spent vs planned)",
            labels = labels,
            series = listOf(
                ChartSeries("Spent", PieChartColors[2], history.map { it.progress?.percent?.toFloat() })
            ),
            valueFormatter = { value -> "${value.toInt()}%" },
            showValueLabels = true
        )
    }
}

@Composable
private fun SnapshotHistoryPage(viewModel: AnalyticsViewModel) {
    val snapshots by viewModel.snapshotHistory.collectAsStateWithLifecycle()
    val currency by viewModel.snapshotCurrency.collectAsStateWithLifecycle()
    var horizon by remember { mutableStateOf(HistoryHorizon.ONE_YEAR) }

    if (snapshots.isEmpty()) {
        EmptyState("No history yet — mark a budget as completed to record your first snapshot.")
        return
    }

    val visible = remember(snapshots, horizon) { horizon.applyTo(snapshots) }

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

        SwipeableTrendChartCard(
            title = "Net worth",
            labels = labels,
            series = listOf(
                ChartSeries("Net worth", PieChartColors[4], visible.map { it.netWorth.toFloat() })
            ),
            valueFormatter = moneyFormatter,
            showValueLabels = true
        )

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
            valueFormatter = moneyFormatter,
            showValueLabels = true
        )
    }
}

@Composable
private fun InvestmentsBreakdownPage(viewModel: AnalyticsViewModel) {
    val categoryBreakdown by viewModel.investmentCategoryBreakdown.collectAsStateWithLifecycle()
    val accountBreakdown by viewModel.investmentAccountBreakdown.collectAsStateWithLifecycle()
    val currencyBreakdown by viewModel.investmentCurrencyBreakdown.collectAsStateWithLifecycle()
    val yearlyHistory by viewModel.investmentYearlyHistory.collectAsStateWithLifecycle()
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
                valueFormatter = moneyFormatter,
                showValueLabels = true
            )

            TrendChartCard(
                title = "Contributions by year",
                labels = labels,
                series = listOf(
                    ChartSeries("Invested", PieChartColors[2], yearlyHistory.map { it.contributions.toFloat() })
                ),
                valueFormatter = moneyFormatter,
                showValueLabels = true
            )
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
