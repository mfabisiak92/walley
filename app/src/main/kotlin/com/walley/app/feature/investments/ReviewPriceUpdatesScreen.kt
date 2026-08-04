package com.walley.app.feature.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import java.math.BigDecimal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPriceUpdatesScreen(
    onNavigateBack: () -> Unit,
    viewModel: UpdatePricesViewModel = hiltViewModel()
) {
    val review by viewModel.currentReview.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (review == null) {
        // No review data, go back
        onNavigateBack()
        return
    }
    val expandedAccounts = remember { mutableStateMapOf<Long, Boolean>() }
    // Tapping a truncated investment name reveals it in full here, rather than hiding it behind
    // navigation — this is a confirmation screen, so nothing about what's being changed should
    // require leaving the screen to see.
    val onRevealFullLabel: (String) -> Unit = { fullText ->
        scope.launch {
            // showSnackbar queues behind whatever's currently showing rather than replacing it, so
            // tapping a second name while the first is still up wouldn't update the text until the
            // first one's duration elapsed. Dismissing first makes the switch immediate.
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(fullText)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                // Tapping the snackbar itself dismisses it early instead of waiting out its duration.
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.clickable { data.dismiss() }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.investments_review_prices_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveReview()
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) { Text(stringResource(R.string.investments_action_confirm_and_save)) }
        }
    ) { innerPadding ->
        val currentReview = review!!
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                TotalsCard(totals = currentReview.totalsByCurrency)
            }
            items(currentReview.accountChanges, key = { it.accountId }) { accountChange ->
                AccountChangeItem(
                    accountChange = accountChange,
                    isExpanded = expandedAccounts[accountChange.accountId] ?: false,
                    onToggleExpand = { expanded ->
                        expandedAccounts[accountChange.accountId] = expanded
                    },
                    onRevealFullLabel = onRevealFullLabel
                )
            }
        }
    }
}

@Composable
private fun TotalsCard(totals: List<CurrencyTotals>) {
    if (totals.isEmpty()) return
    val multiCurrency = totals.size > 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Text(
            text = stringResource(R.string.investments_review_total_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        val accountBalanceLabel = stringResource(R.string.investments_label_account_balance)
        val netBalanceLabel = stringResource(R.string.investments_label_net_balance)
        totals.forEachIndexed { index, currencyTotals ->
            if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val suffix = if (multiCurrency) " (${currencyTotals.currencySymbol})" else ""
            BalanceChangeBlock(
                label = "$accountBalanceLabel$suffix",
                before = currencyTotals.beforeBalance,
                after = currencyTotals.afterBalance,
                change = currencyTotals.balanceChange,
                changePercent = currencyTotals.balanceChangePercent,
                currencySymbol = currencyTotals.currencySymbol
            )
            BalanceChangeBlock(
                label = "$netBalanceLabel$suffix",
                before = currencyTotals.beforeNetBalance,
                after = currencyTotals.afterNetBalance,
                change = currencyTotals.netChange,
                changePercent = currencyTotals.netChangePercent,
                currencySymbol = currencyTotals.currencySymbol,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AccountChangeItem(
    accountChange: AccountBalanceChange,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit,
    onRevealFullLabel: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        // Account header (clickable to expand)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand(!isExpanded) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = accountChange.accountName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) R.string.investments_cd_collapse else R.string.investments_cd_expand
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        BalanceChangeBlock(
            label = stringResource(R.string.investments_label_account_balance),
            before = accountChange.beforeAccountBalance,
            after = accountChange.afterAccountBalance,
            change = accountChange.accountChange,
            changePercent = accountChange.accountChangePercent,
            currencySymbol = accountChange.accountCurrencySymbol,
            modifier = Modifier.padding(top = 8.dp)
        )

        BalanceChangeBlock(
            label = stringResource(R.string.investments_label_net_balance),
            before = accountChange.beforeNetBalance,
            after = accountChange.afterNetBalance,
            change = accountChange.netChange,
            changePercent = accountChange.netChangePercent,
            currencySymbol = accountChange.accountCurrencySymbol,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Expanded content - individual investments
        if (isExpanded && accountChange.investments.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accountChange.investments.forEach { investment ->
                    val fullLabel = "${investment.name} · ${investment.ticker}"
                    BalanceChangeBlock(
                        label = fullLabel,
                        before = investment.beforeBalance,
                        after = investment.afterBalance,
                        change = investment.change,
                        changePercent = investment.changePercent,
                        currencySymbol = accountChange.accountCurrencySymbol,
                        labelMaxWidthFraction = 0.4f,
                        compact = true,
                        onLabelClick = { onRevealFullLabel(fullLabel) }
                    )
                }
            }
        }
    }
}

/**
 * One metric's before/after value plus its change, in the concise pattern used throughout this
 * screen: label on the left with "before → after" on the right, then the change (colored green for
 * a gain, blue for no change, red for a loss) right-aligned on the line below.
 */
@Composable
private fun BalanceChangeBlock(
    label: String,
    before: BigDecimal,
    after: BigDecimal,
    change: BigDecimal,
    changePercent: BigDecimal?,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    labelMaxWidthFraction: Float = 0.3f,
    compact: Boolean = false,
    onLabelClick: (() -> Unit)? = null
) {
    val labelStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val valueStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val changeColor = colorForChange(change)
    // Only the ellipsized case is actually tappable — a label that already fits has nothing more
    // to reveal, so it shouldn't look or behave like an interactive element.
    var isLabelTruncated by remember(label) { mutableStateOf(false) }
    val labelIsTappable = onLabelClick != null && isLabelTruncated

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result -> isLabelTruncated = result.hasVisualOverflow },
                modifier = Modifier
                    .weight(labelMaxWidthFraction)
                    .clickable(enabled = labelIsTappable) { onLabelClick?.invoke() }
            )
            Text(
                text = "${formatMoney(before, currencySymbol)} → ${formatMoney(after, currencySymbol)}",
                style = valueStyle,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f - labelMaxWidthFraction)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = changeText(change, changePercent, currencySymbol),
                style = MaterialTheme.typography.labelSmall,
                color = changeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun colorForChange(change: BigDecimal): Color = when {
    change > BigDecimal.ZERO -> Color(0xFF2E7D32) // Green
    change < BigDecimal.ZERO -> MaterialTheme.colorScheme.error // Red
    else -> Color(0xFF1976D2) // Blue
}

private fun changeText(change: BigDecimal, changePercent: BigDecimal?, currencySymbol: String): String {
    // formatMoney already renders a leading "-" for negative amounts, so a sign is only added for
    // a genuine gain; zero and losses are left to speak for themselves.
    val sign = if (change > BigDecimal.ZERO) "+" else ""
    val amount = "$sign${formatMoney(change, currencySymbol)}"
    return if (changePercent != null) "$amount ($sign${changePercent.toPlainString()}%)" else amount
}
