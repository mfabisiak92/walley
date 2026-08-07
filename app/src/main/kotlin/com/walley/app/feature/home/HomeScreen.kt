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
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.AccountBalanceContainerColor
import com.walley.app.core.ui.AccountBalanceContentColor
import com.walley.app.core.ui.BudgetItemIconBadge
import com.walley.app.core.ui.InvestmentGainColor
import com.walley.app.core.ui.InvestmentNeutralColor
import com.walley.app.core.ui.NetWorthCardContainerColor
import com.walley.app.core.ui.NetWorthCardContentColor
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.core.ui.paidProgressColor
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
    onNavigateToNetWorthDetail: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBackup: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val homeBalances by viewModel.homeBalances.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    val monthBudgetSummary by viewModel.monthBudgetSummary.collectAsStateWithLifecycle()
    val upcomingItems by viewModel.upcomingItems.collectAsStateWithLifecycle()
    val showBackupWarning by viewModel.showBackupWarning.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            WalleyTopBar(
                onTitleClick = {},
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Default.Insights, contentDescription = stringResource(R.string.home_analytics_content_description))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings_content_description))
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
        ) {
            if (showBackupWarning) {
                BackupWarningBanner(onClick = onNavigateToBackup)
            }
            Column(
                modifier = Modifier.padding(16.dp),
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
}

@Composable
private fun BackupWarningBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.backup_warning_reminder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.backup_warning_action))
            }
        }
    }
}

@Composable
private fun NetWorthCard(netWorth: NetWorthState, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NetWorthCardContainerColor,
            contentColor = NetWorthCardContentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(stringResource(R.string.home_net_worth), style = MaterialTheme.typography.titleMedium)
            if (netWorth.amount != null) {
                Text(
                    text = formatMoney(netWorth.amount, netWorth.currency),
                    style = MaterialTheme.typography.headlineLarge
                )
                netWorth.projectedAmount?.let { projected ->
                    Text(
                        text = stringResource(R.string.home_projected, formatMoney(projected, netWorth.currency)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NetWorthCardContentColor.copy(alpha = 0.75f)
                    )
                }
                netWorth.plannedNetWorth?.let { planned ->
                    Text(
                        text = stringResource(R.string.home_planned, formatMoney(planned, netWorth.currency)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NetWorthCardContentColor.copy(alpha = 0.75f)
                    )
                }
                Text(
                    text = monthOverMonthChangeText(netWorth),
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = stringResource(R.string.home_exchange_rates_unavailable),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Change vs. the previous calendar month's net worth, as recorded in that month's financial snapshot —
 * "-" when no snapshot exists (the previous month's budget was never marked completed). Uses
 * [NetWorthState.projectedAmount] when available so the delta reflects where net worth is headed by
 * month end, falling back to the current [NetWorthState.amount] when there's no projection.
 */
private fun monthOverMonthChangeText(netWorth: NetWorthState): String {
    val projected = netWorth.projectedAmount ?: netWorth.amount ?: return "-"
    val previous = netWorth.previousMonthNetWorth ?: return "-"
    val delta = projected - previous
    return (if (delta.signum() > 0) "+" else "") + formatMoney(delta, netWorth.currency)
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
            Text(stringResource(R.string.home_this_months_budget), style = MaterialTheme.typography.titleMedium)
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
                        stringResource(
                            R.string.home_percent_days_left,
                            progress.percent.setScale(0, RoundingMode.HALF_UP).toString(),
                            summary.daysLeftInMonth
                        ),
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
                        stringResource(R.string.home_unallocated, formatMoney(it, summary.currency)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    stringResource(R.string.home_exchange_rate_unavailable_progress),
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
                stringResource(R.string.home_due_soon),
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
                    item.daysUntilDue < 0 -> stringResource(R.string.home_overdue) to MaterialTheme.colorScheme.error
                    item.daysUntilDue == 0 -> stringResource(R.string.home_due_today) to MaterialTheme.colorScheme.error
                    item.daysUntilDue == 1 -> stringResource(R.string.home_due_tomorrow) to MaterialTheme.colorScheme.onSurfaceVariant
                    else -> stringResource(R.string.home_due_in_days, item.daysUntilDue) to MaterialTheme.colorScheme.onSurfaceVariant
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
    if (balances.checkingCash.isEmpty() && balances.savings.isEmpty() && balances.investments.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (balances.checkingCash.isNotEmpty() || balances.savings.isNotEmpty()) {
            CheckingCashCard(balances)
        }
        if (balances.investments.isNotEmpty()) {
            InvestmentsCard(balances)
        }
    }
}

/**
 * "Checking, cash & savings" section header above a single card — like the Investments grid below it —
 * rather than repeated per currency block. Each block: checking/cash plus savings combined as the hero
 * figure (currency code badged in the top-right corner, since the label above no longer carries it),
 * with Available balance (checking/cash minus any virtual Savings envelope earmarked from it) and
 * Savings (dedicated Saving accounts plus those same envelopes) below as a two-column row. The hero
 * figure is Available + Savings rather than raw checking/cash + Savings: a virtual envelope's balance
 * already sits inside its host's checking/cash balance, so adding both would double-count it.
 */
@Composable
private fun CheckingCashCard(balances: HomeBalances) {
    val currencies = (balances.checkingCash.map { it.currency } + balances.savings.map { it.currency } + balances.availableBalance.map { it.currency })
        .distinct()
    if (currencies.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.home_checking_cash_savings), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AccountBalanceContainerColor, contentColor = AccountBalanceContentColor)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                currencies.forEachIndexed { index, currency ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = AccountBalanceContentColor.copy(alpha = 0.15f)
                        )
                    }
                    val checkingCashTotal = balances.checkingCash.find { it.currency == currency }?.total ?: BigDecimal.ZERO
                    val available = balances.availableBalance.find { it.currency == currency }?.total ?: checkingCashTotal
                    val savings = balances.savings.find { it.currency == currency }?.total ?: BigDecimal.ZERO
                    val total = available + savings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(formatMoney(total, currency), style = MaterialTheme.typography.titleLarge)
                        Text(
                            currency.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = AccountBalanceContentColor.copy(alpha = 0.7f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.home_available), style = MaterialTheme.typography.bodySmall, color = AccountBalanceContentColor.copy(alpha = 0.7f))
                            Text(formatMoney(available, currency), style = MaterialTheme.typography.titleSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.home_savings), style = MaterialTheme.typography.bodySmall, color = AccountBalanceContentColor.copy(alpha = 0.7f))
                            Text(formatMoney(savings, currency), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Investments" section header above a single card, following the same shape as [CheckingCashCard]:
 * one block per currency instead of a grid of separate tiles. Within a block, total balance sits in
 * the top-left corner unlabeled (it's the hero figure), Net balance top-right, Invested amount
 * bottom-left and Uninvested cash bottom-right — all four corners labeled except the hero. The tile
 * color is unchanged from before (still [MaterialTheme.colorScheme.tertiaryContainer]); only the
 * Net balance value keeps its gain/loss tint (green/red/blue).
 */
@Composable
private fun InvestmentsCard(balances: HomeBalances) {
    val currencies = (
        balances.investments.map { it.currency } +
            balances.investmentsAfterTax.map { it.currency } +
            balances.investedAmount.map { it.currency } +
            balances.uninvestedCash.map { it.currency }
        ).distinct()
    if (currencies.isEmpty()) return

    val lossColor = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    fun gainLossColor(currency: Currency): Color {
        val gainLoss = balances.investmentsGainLoss.find { it.currency == currency }?.total ?: BigDecimal.ZERO
        return when {
            gainLoss.signum() > 0 -> InvestmentGainColor
            gainLoss.signum() < 0 -> lossColor
            else -> InvestmentNeutralColor
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.home_investments), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                currencies.forEachIndexed { index, currency ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    val total = balances.investments.find { it.currency == currency }?.total ?: BigDecimal.ZERO
                    val net = balances.investmentsAfterTax.find { it.currency == currency }?.total ?: total
                    val invested = balances.investedAmount.find { it.currency == currency }?.total ?: BigDecimal.ZERO
                    val uninvested = balances.uninvestedCash.find { it.currency == currency }?.total ?: BigDecimal.ZERO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(formatMoney(total, currency), style = MaterialTheme.typography.titleLarge)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.home_net_balance), style = MaterialTheme.typography.bodySmall, color = labelColor)
                            Text(
                                formatMoney(net, currency),
                                style = MaterialTheme.typography.titleSmall,
                                color = gainLossColor(currency)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.home_invested_amount), style = MaterialTheme.typography.bodySmall, color = labelColor)
                            Text(formatMoney(invested, currency), style = MaterialTheme.typography.titleSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.home_uninvested_cash), style = MaterialTheme.typography.bodySmall, color = labelColor)
                            Text(formatMoney(uninvested, currency), style = MaterialTheme.typography.titleSmall)
                        }
                    }
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
            Text(stringResource(R.string.home_by_currency), style = MaterialTheme.typography.titleMedium)
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
