package com.walley.app.feature.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.CurrencyTotal
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    onOpenUpdateBalances: (AccountBalanceGroup) -> Unit,
    onOpenCashOperations: (Long) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val tabs = AccountBalanceGroup.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = { WalleyTopBar(onTitleClick = onNavigateHome) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, group ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(group.label) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AccountsListPage(
                    viewModel = viewModel,
                    group = tabs[page],
                    onOpenUpdateBalances = onOpenUpdateBalances,
                    onOpenCashOperations = onOpenCashOperations
                )
            }
        }
    }
}

@Composable
private fun AccountsListPage(
    viewModel: AccountsViewModel,
    group: AccountBalanceGroup,
    onOpenUpdateBalances: (AccountBalanceGroup) -> Unit,
    onOpenCashOperations: (Long) -> Unit
) {
    val allowedTypes = group.types
    val tabLabel = group.label
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val filteredAccounts = accounts.filter { it.type in allowedTypes }
    val activeAccounts = filteredAccounts.filterNot { it.isClosed }
    val portfolioTaxEstimate by viewModel.portfolioTaxEstimate.collectAsStateWithLifecycle()
    val investmentsNetProfit by viewModel.investmentsNetProfit.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()
    val closeBlockedMessage by viewModel.closeBlockedMessage.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var showClosed by remember { mutableStateOf(false) }

    val closedCount = filteredAccounts.size - activeAccounts.size
    val visibleAccounts = if (showClosed) filteredAccounts else activeAccounts

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (filteredAccounts.isNotEmpty()) {
                    SmallFloatingActionButton(onClick = { onOpenUpdateBalances(group) }) {
                        Icon(Icons.Default.EditNote, contentDescription = "Update balances")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add account")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (closedCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilterChip(
                        selected = showClosed,
                        onClick = { showClosed = !showClosed },
                        label = { Text(if (showClosed) "Hide closed" else "Show closed ($closedCount)") }
                    )
                }
            }
            if (filteredAccounts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AccountBox,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No $tabLabel accounts yet — tap + to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (visibleAccounts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AccountBox,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "All accounts are closed — tap \"Show closed\" above to see them.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isInvestments = group == AccountBalanceGroup.INVESTMENTS
                        SummaryRibbon(
                            totalsByCurrency = currencyTotals(activeAccounts.filterNot { it.isVirtual }),
                            netProfit = if (isInvestments) investmentsNetProfit else null,
                            estimatedTax = if (isInvestments) portfolioTaxEstimate.taxOwed else null,
                            baseCurrency = baseCurrency
                        )
                    }
                    items(visibleAccounts, key = { it.id }) { account ->
                        AccountRow(
                            account = account,
                            onOpenCashOperations = { onOpenCashOperations(account.id) },
                            onEditAccount = { editingAccount = account },
                            onSetDefault = { viewModel.setDefaultAccount(account.id) },
                            onReopen = { viewModel.reopenAccount(account.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            allowedTypes = allowedTypes,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, currency, balance, taxRate, targetAmount, commissionFlat, commissionPercent, isVirtual ->
                viewModel.addAccount(
                    name,
                    type,
                    currency,
                    balance,
                    taxRate,
                    targetAmount,
                    commissionFlat,
                    commissionPercent,
                    isVirtual
                )
                showAddDialog = false
            }
        )
    }

    editingAccount?.let { account ->
        EditAccountDialog(
            account = account,
            allowedTypes = allowedTypes,
            otherAccounts = accounts.filter { it.id != account.id },
            onDismiss = { editingAccount = null },
            onSave = { name, type, taxRate, newBalance, targetAmount, commissionFlat, commissionPercent, isVirtual ->
                viewModel.updateAccount(
                    account.id,
                    name,
                    type,
                    taxRate,
                    newBalance,
                    targetAmount,
                    commissionFlat,
                    commissionPercent,
                    isVirtual
                )
                editingAccount = null
            },
            onDelete = {
                viewModel.deleteAccount(account.id)
                editingAccount = null
            },
            onClose = { transferToAccountId ->
                viewModel.closeAccount(account.id, transferToAccountId)
                editingAccount = null
            }
        )
    }

    deleteBlockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBlockedMessage,
            title = { Text("Can't delete account") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteBlockedMessage) { Text("OK") }
            }
        )
    }

    closeBlockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissCloseBlockedMessage,
            title = { Text("Can't close account") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissCloseBlockedMessage) { Text("OK") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountRow(
    account: Account,
    onOpenCashOperations: () -> Unit,
    onEditAccount: () -> Unit,
    onSetDefault: () -> Unit,
    onReopen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onEditAccount.takeUnless { account.isClosed },
                onClick = onOpenCashOperations
            ),
        colors = if (account.isClosed) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val canBeDefault = account.type == AccountType.CHECKING || account.type == AccountType.CASH
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(account.name, style = MaterialTheme.typography.titleMedium)
                    val subtitle = listOfNotNull(
                        // Redundant within the Savings/Investments tabs, which are already single-type —
                        // only Cash & Checking mixes both types, so the label still disambiguates there.
                        account.type.label.takeIf { account.type == AccountType.CHECKING || account.type == AccountType.CASH },
                        account.taxRate.label.takeIf { account.type == AccountType.INVESTMENT },
                        "Virtual".takeIf { account.isVirtual },
                        "Closed".takeIf { account.isClosed }
                    ).joinToString(" · ")
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (account.isClosed) {
                        OutlinedButton(
                            onClick = onReopen,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Reopen", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Text(formatMoney(account.balance, account.currency), style = MaterialTheme.typography.titleMedium)
                        if (canBeDefault) {
                            DefaultAccountButton(isDefault = account.isDefault, onClick = onSetDefault)
                        }
                    }
                }
            }
            Column {
                if (account.type == AccountType.INVESTMENT) {
                    val unrealizedGain = account.investmentGainLoss
                    val netUnrealizedGain = account.investmentNetProfit ?: unrealizedGain
                    InvestmentDetailLine("Current account balance", account.balance, account.currency)
                    InvestmentDetailLine("Uninvested cash", account.uninvestedCash, account.currency)
                    InvestmentDetailLine("Unrealized gain", unrealizedGain, account.currency, signed = true)
                    InvestmentDetailLine("Net unrealized gain", netUnrealizedGain, account.currency, signed = true)
                }
                val progressPercent = account.targetProgressPercent
                if (progressPercent != null && !account.isClosed) {
                    SavingsGoalProgress(account = account, progressPercent = progressPercent)
                }
            }
        }
    }
}

@Composable
private fun InvestmentDetailLine(label: String, value: BigDecimal, currency: Currency, signed: Boolean = false) {
    Text(
        text = "$label: ${formatMoney(value, currency)}",
        style = MaterialTheme.typography.bodySmall,
        color = when {
            !signed -> MaterialTheme.colorScheme.onSurfaceVariant
            value.signum() > 0 -> Color(0xFF2E7D32)
            value.signum() < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

/** Marks/unmarks this [AccountType.CHECKING]/[AccountType.CASH] account as the one used by default — a concept that only makes sense for everyday spending accounts, not savings goals or investment accounts. */
@Composable
private fun DefaultAccountButton(isDefault: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        if (isDefault) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Default account",
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                Icons.Outlined.StarOutline,
                contentDescription = "Set as default account",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Callers should exclude virtual accounts first — their balance is already counted in a real account. */
private fun currencyTotals(accounts: List<Account>): List<CurrencyTotal> =
    Currency.entries.mapNotNull { currency ->
        val total = accounts.filter { it.currency == currency }.fold(BigDecimal.ZERO) { acc, a -> acc + a.balance }
        if (total.signum() == 0) null else CurrencyTotal(currency, total)
    }

/**
 * Small at-a-glance strip above a tab's account list — total balance, plus net profit and estimated
 * tax for Investments. Deliberately flat (tinted fill, no elevation or border) and slimmer than the
 * account cards below it, so it reads as a summary header rather than another list item.
 */
@Composable
private fun SummaryRibbon(
    totalsByCurrency: List<CurrencyTotal>,
    netProfit: BigDecimal?,
    estimatedTax: BigDecimal?,
    baseCurrency: Currency
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val tint = MaterialTheme.colorScheme.onPrimaryContainer
            val totalText = if (totalsByCurrency.isEmpty()) {
                formatMoney(BigDecimal.ZERO, baseCurrency)
            } else {
                totalsByCurrency.joinToString(" · ") { formatMoney(it.total, it.currency) }
            }
            RibbonStat("Total", totalText, tint)
            if (netProfit != null) {
                RibbonStat("Profit", formatMoney(netProfit, baseCurrency), tint)
            }
            if (estimatedTax != null) {
                RibbonStat("Tax", formatMoney(estimatedTax, baseCurrency), tint)
            }
        }
    }
}

/** Equal-width column so 3 stats never squeeze unevenly — a long value wraps onto a second line instead of being hidden or forcing a sibling to shrink. */
@Composable
private fun RowScope.RibbonStat(label: String, value: String, tint: Color) {
    Column(modifier = Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = tint)
    }
}

@Composable
private fun SavingsGoalProgress(account: Account, progressPercent: BigDecimal) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        LinearProgressIndicator(
            progress = { progressPercent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (account.targetReached) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${progressPercent.setScale(0, RoundingMode.HALF_UP)}% of " +
                    formatMoney(account.targetAmount ?: BigDecimal.ZERO, account.currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (account.targetReached) {
                Text(
                    "Goal reached",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}
