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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.estimatedTaxForYear
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
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
    val investmentsByAccount by viewModel.investmentsByAccount.collectAsStateWithLifecycle()
    val portfolioTaxEstimate by viewModel.portfolioTaxEstimate.collectAsStateWithLifecycle()
    val investmentsNetProfit by viewModel.investmentsNetProfit.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

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
        if (filteredAccounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isInvestments = group == AccountBalanceGroup.INVESTMENTS
                    SummaryRibbon(
                        totalsByCurrency = currencyTotals(filteredAccounts.filterNot { it.isVirtual }),
                        netProfit = if (isInvestments) investmentsNetProfit else null,
                        estimatedTax = if (isInvestments) portfolioTaxEstimate.taxOwed else null,
                        baseCurrency = baseCurrency
                    )
                }
                items(filteredAccounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        investmentsInAccount = investmentsByAccount[account.id].orEmpty(),
                        onOpenCashOperations = { onOpenCashOperations(account.id) },
                        onEditAccount = { editingAccount = account },
                        onSetDefault = { viewModel.setDefaultAccount(account.id) }
                    )
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountRow(
    account: Account,
    investmentsInAccount: List<InvestmentWithTransactions>,
    onOpenCashOperations: () -> Unit,
    onEditAccount: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onEditAccount, onClick = onOpenCashOperations),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val canBeDefault = account.type == AccountType.CHECKING || account.type == AccountType.CASH
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(account.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = listOfNotNull(
                            account.type.label,
                            account.taxRate.label.takeIf { account.type == AccountType.INVESTMENT },
                            "Virtual".takeIf { account.isVirtual }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoney(account.balance, account.currency), style = MaterialTheme.typography.titleMedium)
                    if (canBeDefault) {
                        DefaultAccountButton(isDefault = account.isDefault, onClick = onSetDefault)
                    }
                }
            }
            Column {
                if (account.type == AccountType.INVESTMENT) {
                    Text(
                        text = "Uninvested: ${formatMoney(account.uninvestedCash, account.currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val gainLoss = account.investmentGainLoss
                    if (gainLoss.signum() != 0) {
                        Text(
                            text = if (gainLoss.signum() > 0) {
                                "Gain: ${formatMoney(gainLoss, account.currency)}"
                            } else {
                                "Loss: ${formatMoney(gainLoss.abs(), account.currency)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gainLoss.signum() > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                        account.investmentTaxAmount?.let { tax ->
                            Text(
                                text = "Estimated tax (${account.taxRate.label}): ${formatMoney(tax, account.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1565C0)
                            )
                        }
                        account.investmentNetProfit?.let { netProfit ->
                            Text(
                                text = "Net profit: ${formatMoney(netProfit, account.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    account.estimatedTaxForYear(investmentsInAccount, LocalDate.now().year)?.let { estimatedTax ->
                        Text(
                            text = "Owed tax next year: ${formatMoney(estimatedTax, account.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1565C0)
                        )
                    }
                }
                val progressPercent = account.targetProgressPercent
                if (progressPercent != null) {
                    SavingsGoalProgress(account = account, progressPercent = progressPercent)
                }
            }
        }
    }
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
