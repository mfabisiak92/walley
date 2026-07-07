package com.walley.app.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()

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
        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
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
                    "No budgets yet — create one to see analytics.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
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
    }
}
