package com.walley.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.BudgetItemIconBadge
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.core.ui.paidProgressColor
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.CurrencyTotal
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
    onNavigateToNetWorthDetail: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val homeBalances by viewModel.homeBalances.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    val monthBudgetSummary by viewModel.monthBudgetSummary.collectAsStateWithLifecycle()
    val upcomingItems by viewModel.upcomingItems.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            WalleyTopBar(
                onTitleClick = {},
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Default.Insights, contentDescription = "Analytics")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            netWorth?.let { NetWorthCard(it, onClick = onNavigateToNetWorthDetail) }
            monthBudgetSummary?.let { MonthBudgetCard(it) }
            if (upcomingItems.isNotEmpty()) {
                UpcomingItemsCard(upcomingItems)
            }
            BalanceStatsRow(homeBalances)
            netWorth?.let { if (it.breakdown.isNotEmpty()) CurrencyBreakdownCard(it.breakdown) }
        }
    }
}

@Composable
private fun NetWorthCard(netWorth: NetWorthState, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Net worth", style = MaterialTheme.typography.titleMedium)
            if (netWorth.amount != null) {
                Text(
                    text = formatMoney(netWorth.amount, netWorth.currency),
                    style = MaterialTheme.typography.headlineLarge
                )
                netWorth.projectedAmount?.let { projected ->
                    Text(
                        text = "Projected · ${formatMoney(projected, netWorth.currency)} end of month",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "Exchange rates unavailable",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun MonthBudgetCard(summary: MonthBudgetSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("This month's budget", style = MaterialTheme.typography.titleMedium)
            val progress = summary.progress
            if (progress != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${formatMoney(progress.spent, summary.currency)} / ${formatMoney(progress.planned, summary.currency)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${progress.percent.setScale(0, RoundingMode.HALF_UP)}% · ${summary.daysLeftInMonth} days left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val spentFraction = progress.percent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
                    .toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { spentFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = paidProgressColor(spentFraction)
                )
                summary.unallocated?.let {
                    Text(
                        "${formatMoney(it, summary.currency)} unallocated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "Exchange rate unavailable — progress can't be computed right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun UpcomingItemsCard(items: List<UpcomingBudgetItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Text(
                "Due soon",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
            )
            items.take(3).forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider()
                UpcomingItemRow(item)
            }
        }
    }
}

@Composable
private fun UpcomingItemRow(item: UpcomingBudgetItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            UpcomingItemIconBadge(icon = item.icon)
            Column {
                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                val (label, color) = when {
                    item.daysUntilDue < 0 -> "Overdue" to MaterialTheme.colorScheme.error
                    item.daysUntilDue == 0 -> "Due today" to MaterialTheme.colorScheme.error
                    item.daysUntilDue == 1 -> "Due tomorrow" to MaterialTheme.colorScheme.onSurfaceVariant
                    else -> "Due in ${item.daysUntilDue} days" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
        Text(formatMoney(item.amount, item.currency), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Falls back to a neutral circle when the item has no icon, so "Due soon" always shows a badge. */
@Composable
private fun UpcomingItemIconBadge(icon: BudgetItemIcon?, size: androidx.compose.ui.unit.Dp = 28.dp) {
    if (icon != null) {
        BudgetItemIconBadge(icon = icon, size = size)
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

@Composable
private fun BalanceStatsRow(balances: HomeBalances) {
    if (balances.total.isEmpty() && balances.savings.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(title = "Total balance", currencyTotals = balances.total, modifier = Modifier.weight(1f))
        StatCard(title = "Savings", currencyTotals = balances.savings, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(title: String, currencyTotals: List<CurrencyTotal>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (currencyTotals.isEmpty()) {
                Text("—", style = MaterialTheme.typography.titleMedium)
            } else {
                currencyTotals.forEach { currencyTotal ->
                    Text(formatMoney(currencyTotal.total, currencyTotal.currency), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun CurrencyBreakdownCard(breakdown: List<NetWorthByCurrency>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("By currency", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                breakdown.forEachIndexed { index, slice ->
                    val weight = slice.percent.toFloat().coerceAtLeast(0.1f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .background(PieChartColors[index % PieChartColors.size])
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                breakdown.forEachIndexed { index, slice ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PieChartColors[index % PieChartColors.size])
                        )
                        Text(
                            "${slice.currency.name} ${slice.percent.setScale(0, RoundingMode.HALF_UP)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
