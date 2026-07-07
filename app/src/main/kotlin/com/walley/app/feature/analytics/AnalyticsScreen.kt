package com.walley.app.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.ChartSeries
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.TrendChartCard
import java.math.BigDecimal
import kotlinx.coroutines.launch

private val TABS = listOf("Budget", "History")

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
                    else -> SnapshotHistoryPage(viewModel)
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

    if (snapshots.isEmpty()) {
        EmptyState("No history yet — mark a budget as completed to record your first snapshot.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val labels = snapshots.map { it.label }
        val moneyFormatter = { value: Float -> formatMoney(BigDecimal.valueOf(value.toDouble()), currency) }

        TrendChartCard(
            title = "Account balances",
            labels = labels,
            series = listOf(
                ChartSeries("Cash & Checking", PieChartColors[0], snapshots.map { it.cashAndChecking.toFloat() }),
                ChartSeries("Savings", PieChartColors[1], snapshots.map { it.savings.toFloat() }),
                ChartSeries("Investments", PieChartColors[2], snapshots.map { it.investments.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        TrendChartCard(
            title = "Net worth",
            labels = labels,
            series = listOf(
                ChartSeries("Net worth", PieChartColors[4], snapshots.map { it.netWorth.toFloat() })
            ),
            valueFormatter = moneyFormatter,
            showValueLabels = true
        )

        TrendChartCard(
            title = "Income by source",
            labels = labels,
            series = listOf(
                ChartSeries("Salary", PieChartColors[0], snapshots.map { it.salaryIncome.toFloat() }),
                ChartSeries("Dividends", PieChartColors[1], snapshots.map { it.dividendsIncome.toFloat() }),
                ChartSeries("Interest", PieChartColors[3], snapshots.map { it.interestIncome.toFloat() }),
                ChartSeries("Other", PieChartColors[5], snapshots.map { it.otherIncome.toFloat() })
            ),
            valueFormatter = moneyFormatter
        )

        TrendChartCard(
            title = "Investment growth (net of contributions)",
            labels = labels,
            series = listOf(
                ChartSeries("Growth", PieChartColors[5], snapshots.map { it.investmentGrowth?.toFloat() })
            ),
            valueFormatter = moneyFormatter,
            showValueLabels = true
        )
    }
}
