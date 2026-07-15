package com.walley.app.feature.accounts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.VerticalDivider
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
import com.walley.app.core.ui.AccountBalanceContainerColor
import com.walley.app.core.ui.AccountBalanceContentColor
import com.walley.app.core.ui.InvestmentGainColor
import com.walley.app.core.ui.InvestmentNeutralColor
import com.walley.app.core.ui.RemovableChip
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.CurrencyTotal
import com.walley.app.domain.model.SortDirection
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
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
    val currentGroup = tabs[pagerState.currentPage]

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val sortState by viewModel.sortState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState(currentGroup).collectAsStateWithLifecycle()
    var showSortFilterSheet by remember { mutableStateOf(false) }

    val hasActiveSortOrFilter = !sortState.isDefault || !filterState.isDefault

    Scaffold(
        modifier = modifier,
        topBar = {
            WalleyTopBar(
                onTitleClick = onNavigateHome,
                actions = {
                    IconButton(onClick = { showSortFilterSheet = true }) {
                        BadgedBox(badge = { if (hasActiveSortOrFilter) Badge() }) {
                            Icon(Icons.Filled.Tune, contentDescription = "Sort and filter")
                        }
                    }
                }
            )
        }
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
            if (hasActiveSortOrFilter) {
                ActiveFiltersRow(
                    sortState = sortState,
                    filterState = filterState,
                    onResetSort = viewModel::resetSort,
                    onStatusReset = { viewModel.setStatusFilter(currentGroup, AccountStatusFilter.ACTIVE) },
                    onCurrencyToggled = { currency -> viewModel.toggleCurrencyFilter(currentGroup, currency) },
                    onTypeToggled = { type -> viewModel.toggleTypeFilter(currentGroup, type) },
                    onKindReset = { viewModel.setKindFilter(currentGroup, AccountKindFilter.ALL) },
                    onResetAll = {
                        viewModel.resetSort()
                        viewModel.resetFilters(currentGroup)
                    }
                )
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

    if (showSortFilterSheet) {
        val availableCurrencies = accounts.filter { it.type in currentGroup.types }.map { it.currency }.distinct()
        AccountsSortFilterSheet(
            group = currentGroup,
            sortState = sortState,
            filterState = filterState,
            availableCurrencies = availableCurrencies,
            onSortFieldSelected = viewModel::setSortField,
            onSortDirectionSelected = viewModel::setSortDirection,
            onStatusSelected = { status -> viewModel.setStatusFilter(currentGroup, status) },
            onCurrencyToggled = { currency -> viewModel.toggleCurrencyFilter(currentGroup, currency) },
            onTypeToggled = { type -> viewModel.toggleTypeFilter(currentGroup, type) },
            onKindSelected = { kind -> viewModel.setKindFilter(currentGroup, kind) },
            onReset = {
                viewModel.resetSort()
                viewModel.resetFilters(currentGroup)
            },
            onDismiss = { showSortFilterSheet = false }
        )
    }
}

/** Removable-chip summary of whatever's non-default, shown under the tabs so a persisted filter is never silently invisible. */
@Composable
private fun ActiveFiltersRow(
    sortState: AccountsSortState,
    filterState: AccountsFilterState,
    onResetSort: () -> Unit,
    onStatusReset: () -> Unit,
    onCurrencyToggled: (Currency) -> Unit,
    onTypeToggled: (AccountType) -> Unit,
    onKindReset: () -> Unit,
    onResetAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!sortState.isDefault) {
            RemovableChip(label = sortChipLabel(sortState), onRemove = onResetSort)
        }
        if (filterState.status != AccountStatusFilter.ACTIVE) {
            val label = if (filterState.status == AccountStatusFilter.CLOSED) "Closed" else "All statuses"
            RemovableChip(label = label, onRemove = onStatusReset)
        }
        filterState.currencies.forEach { currency ->
            RemovableChip(label = currency.name, onRemove = { onCurrencyToggled(currency) })
        }
        filterState.types.forEach { type ->
            RemovableChip(label = type.label, onRemove = { onTypeToggled(type) })
        }
        if (filterState.kind != AccountKindFilter.ALL) {
            val label = if (filterState.kind == AccountKindFilter.REAL) "Real" else "Virtual"
            RemovableChip(label = label, onRemove = onKindReset)
        }
        TextButton(onClick = onResetAll) { Text("Reset all") }
    }
}

private fun sortChipLabel(sort: AccountsSortState): String {
    val fieldLabel = when (sort.field) {
        AccountSortField.NAME -> "Name"
        AccountSortField.BALANCE -> "Balance"
        AccountSortField.DATE_ADDED -> "Date added"
        AccountSortField.DEFAULT_FIRST -> return "Default first"
    }
    val arrow = if (sort.direction == SortDirection.DESC) "↓" else "↑"
    return "$arrow $fieldLabel"
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
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val exchangeRates by viewModel.exchangeRates.collectAsStateWithLifecycle()
    val sortState by viewModel.sortState.collectAsStateWithLifecycle()
    val userFilterState by viewModel.filterState(group).collectAsStateWithLifecycle()
    val savingsPaidThisMonth by viewModel.savingsPaidThisMonth.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()
    val closeBlockedMessage by viewModel.closeBlockedMessage.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    val visibleAccounts = sortAccounts(filterAccounts(filteredAccounts, userFilterState), sortState, baseCurrency, exchangeRates)

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
                        "No accounts match your filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { viewModel.resetFilters(group) }) {
                        Text("Reset filters")
                    }
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
                        when (group) {
                            AccountBalanceGroup.CASH_CHECKING -> {
                                val checkingCashAccounts = activeAccounts.filterNot { it.isVirtual }
                                // Virtual Savings envelopes are listed on the Savings tab, but their money
                                // physically sits inside one of the accounts shown here — see SplitSummaryHeader.
                                val virtualSavingsAccounts = accounts.filter { it.isVirtual && it.type == AccountType.SAVING && !it.isClosed }
                                // Driven by which currencies actually have a checking/cash account, rather than
                                // currencyTotals' usual "drop zero totals" rule — so Available still shows a
                                // 0 line for a currency whose envelopes exactly cancel out its checking/cash total.
                                val currencies = checkingCashAccounts.map { it.currency }.distinct()
                                SplitSummaryHeader(
                                    leftLabel = "Total",
                                    leftTotals = signedTotalsByCurrency(currencies, checkingCashAccounts) { it.balance },
                                    rightLabel = "Available",
                                    rightTotals = signedTotalsByCurrency(currencies, checkingCashAccounts + virtualSavingsAccounts) { account ->
                                        if (account.isVirtual) -account.balance else account.balance
                                    },
                                    baseCurrency = baseCurrency
                                )
                            }
                            AccountBalanceGroup.SAVINGS -> {
                                val realSavingsAccounts = activeAccounts.filterNot { it.isVirtual }
                                val virtualSavingsAccounts = activeAccounts.filter { it.isVirtual }
                                val currencies = activeAccounts.map { it.currency }.distinct()
                                SplitSummaryHeader(
                                    leftLabel = "Standard accounts",
                                    leftTotals = signedTotalsByCurrency(currencies, realSavingsAccounts) { it.balance },
                                    rightLabel = "Virtual accounts",
                                    rightTotals = signedTotalsByCurrency(currencies, virtualSavingsAccounts) { it.balance },
                                    baseCurrency = baseCurrency
                                )
                            }
                            AccountBalanceGroup.INVESTMENTS -> {
                                InvestmentSummaryCard(activeAccounts.filterNot { it.isVirtual }.filter { it.currency == baseCurrency })
                            }
                        }
                    }
                    items(visibleAccounts, key = { it.id }) { account ->
                        AccountRow(
                            account = account,
                            paidThisMonth = savingsPaidThisMonth[account.id],
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
            onConfirm = { name, type, currency, balance, taxRate, targetAmount, targetDate, commissionFlat, commissionPercent, isVirtual ->
                viewModel.addAccount(
                    name,
                    type,
                    currency,
                    balance,
                    taxRate,
                    targetAmount,
                    targetDate,
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
            onSave = { name, type, taxRate, newBalance, targetAmount, targetDate, commissionFlat, commissionPercent, isVirtual ->
                viewModel.updateAccount(
                    account.id,
                    name,
                    type,
                    taxRate,
                    newBalance,
                    targetAmount,
                    targetDate,
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
    paidThisMonth: BigDecimal?,
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
            if (account.type == AccountType.INVESTMENT) {
                InvestmentAccountBody(account = account, onReopen = onReopen)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(account.name, style = MaterialTheme.typography.titleMedium)
                        val subtitle = listOfNotNull(
                            // Redundant within the Savings tab, which is already single-type — only
                            // Cash & Checking mixes both types, so the label still disambiguates there.
                            account.type.label.takeIf { account.type == AccountType.CHECKING || account.type == AccountType.CASH },
                            account.targetDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
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
                if (paidThisMonth != null && !account.isClosed) {
                    MonthPaidRow(amount = paidThisMonth, currency = account.currency)
                }
                val progressPercent = account.targetProgressPercent
                if (progressPercent != null && !account.isClosed) {
                    SavingsGoalProgress(account = account, progressPercent = progressPercent)
                }
            }
        }
    }
}

/**
 * Investments tab's row layout — name top-left (no tax-rate subtitle; the account already lives on a
 * single-type tab), then two labeled stat rows and a profit line mirroring [InvestmentSummaryCard]'s
 * layout and labels one account at a time: Net balance top-right, Invested amount/Uninvested cash
 * below, and unrealized profit (with its % return on cost basis) flush right below that. All three
 * money figures share [profitColor] — green for a gain, [MaterialTheme.colorScheme.error] for a loss,
 * [InvestmentNeutralColor] when flat — same three-way split [InvestmentSummaryCard] uses.
 */
@Composable
private fun InvestmentAccountBody(account: Account, onReopen: () -> Unit) {
    val invested = account.balance - account.uninvestedCash
    val profit = account.investmentNetProfit ?: account.investmentGainLoss
    val profitColor = when {
        profit.signum() > 0 -> InvestmentGainColor
        profit.signum() < 0 -> MaterialTheme.colorScheme.error
        else -> InvestmentNeutralColor
    }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val profitPercent = account.investmentCostBasis.takeIf { it.signum() > 0 }
        ?.let { costBasis -> profit.divide(costBasis, 4, RoundingMode.HALF_UP) * BigDecimal(100) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(account.name, style = MaterialTheme.typography.titleMedium)
            if (account.isClosed) {
                Text("Closed", style = MaterialTheme.typography.bodySmall, color = labelColor)
            }
        }
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
            Column(horizontalAlignment = Alignment.End) {
                Text("Net balance", style = MaterialTheme.typography.bodySmall, color = labelColor)
                Text(
                    formatMoney(account.netWorthValue, account.currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = profitColor
                )
            }
        }
    }
    if (!account.isClosed) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Invested amount", style = MaterialTheme.typography.bodySmall, color = labelColor)
                Text(formatMoney(invested, account.currency), style = MaterialTheme.typography.titleSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Uninvested cash", style = MaterialTheme.typography.bodySmall, color = labelColor)
                Text(formatMoney(account.uninvestedCash, account.currency), style = MaterialTheme.typography.titleSmall)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            val percentText = profitPercent?.let { " · ${it.setScale(2, RoundingMode.HALF_UP)}%" }.orEmpty()
            Text(
                "${formatMoney(profit, account.currency)}$percentText",
                style = MaterialTheme.typography.bodyMedium,
                color = profitColor
            )
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
 * Like [currencyTotals], but always includes exactly [currencies] — even a currency whose total nets
 * to zero — instead of dropping zero totals. Used for [CashSummaryHeader] so Total and Available stay
 * aligned line-for-line even when a currency's envelopes exactly cancel out its checking/cash total.
 */
private fun signedTotalsByCurrency(currencies: List<Currency>, accounts: List<Account>, amount: (Account) -> BigDecimal): List<CurrencyTotal> =
    currencies.map { currency ->
        CurrencyTotal(currency, accounts.filter { it.currency == currency }.fold(BigDecimal.ZERO) { acc, a -> acc + amount(a) })
    }

/**
 * Two-stat summary header used on the Cash & Checking and Savings tabs: split into two equal halves
 * by a thin divider, one currency per line (no separator joining them), using the same light blue
 * accent as the Home screen's "Checking, cash & savings" card — distinct from the Investments tab's
 * [InvestmentSummaryCard] and from the account cards below it.
 *
 * - Cash & Checking: Total (what the list below sums to) vs. Available balance (that total minus any
 *   virtual Savings envelope earmarked from it — those envelopes are listed on the Savings tab even
 *   though their money physically sits in an account shown here, so this is context the list itself
 *   can't show).
 * - Savings: the real, dedicated Saving accounts' total vs. the virtual envelopes' total — since this
 *   tab's list mixes both, without this split there's no way to tell from the list alone how much of
 *   "Savings" is actual separate money versus money already counted inside a checking/cash account.
 */
@Composable
private fun SplitSummaryHeader(
    leftLabel: String,
    leftTotals: List<CurrencyTotal>,
    rightLabel: String,
    rightTotals: List<CurrencyTotal>,
    baseCurrency: Currency
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AccountBalanceContainerColor,
        contentColor = AccountBalanceContentColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(leftLabel, style = MaterialTheme.typography.labelSmall, color = AccountBalanceContentColor.copy(alpha = 0.75f))
                SplitSummaryAmounts(leftTotals, baseCurrency)
            }
            VerticalDivider(color = AccountBalanceContentColor.copy(alpha = 0.15f))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(rightLabel, style = MaterialTheme.typography.labelSmall, color = AccountBalanceContentColor.copy(alpha = 0.75f))
                SplitSummaryAmounts(rightTotals, baseCurrency)
            }
        }
    }
}

@Composable
private fun SplitSummaryAmounts(totals: List<CurrencyTotal>, baseCurrency: Currency) {
    if (totals.isEmpty()) {
        Text(formatMoney(BigDecimal.ZERO, baseCurrency), style = MaterialTheme.typography.titleSmall)
    } else {
        totals.forEach { Text(formatMoney(it.total, it.currency), style = MaterialTheme.typography.titleSmall) }
    }
}

/**
 * Investments tab summary — same tile as the Home screen's Investments card: one block per currency,
 * Total balance unlabeled in the top-left corner (the hero figure), Net balance top-right (labeled,
 * tinted by gain/loss), Invested amount bottom-left and Uninvested cash bottom-right (both labeled).
 * Same [MaterialTheme.colorScheme.tertiaryContainer] tint as before, distinct from
 * [SplitSummaryHeader]'s light blue used on the other two tabs.
 */
@Composable
private fun InvestmentSummaryCard(accounts: List<Account>) {
    val currencies = accounts.map { it.currency }.distinct()
    if (currencies.isEmpty()) return

    val lossColor = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    fun gainLossColor(currency: Currency): Color {
        val gainLoss = accounts.filter { it.currency == currency }.sumOf { it.investmentGainLoss }
        return when {
            gainLoss.signum() > 0 -> InvestmentGainColor
            gainLoss.signum() < 0 -> lossColor
            else -> InvestmentNeutralColor
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            currencies.forEachIndexed { index, currency ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                val currencyAccounts = accounts.filter { it.currency == currency }
                val total = currencyAccounts.sumOf { it.balance }
                val net = currencyAccounts.sumOf { it.netWorthValue }
                val invested = currencyAccounts.sumOf { it.balance - it.uninvestedCash }
                val uninvested = currencyAccounts.sumOf { it.uninvestedCash }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(formatMoney(total, currency), style = MaterialTheme.typography.titleLarge)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net balance", style = MaterialTheme.typography.bodySmall, color = labelColor)
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
                        Text("Invested amount", style = MaterialTheme.typography.bodySmall, color = labelColor)
                        Text(formatMoney(invested, currency), style = MaterialTheme.typography.titleSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Uninvested cash", style = MaterialTheme.typography.bodySmall, color = labelColor)
                        Text(formatMoney(uninvested, currency), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

/**
 * "Paid this month" — how much has been contributed so far to this Saving account's linked SAVINGS
 * budget item(s) in the current month's budget. A dedicated row, set off by a divider, label on the
 * left and the amount on the right — distinct from [SavingsGoalProgress] below it, which tracks the
 * account's overall balance against its long-term target rather than this month's contribution.
 */
@Composable
private fun MonthPaidRow(amount: BigDecimal, currency: Currency) {
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Paid this month",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            formatMoney(amount, currency),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${progressPercent.setScale(0, RoundingMode.HALF_UP)}% of " +
                    formatMoney(account.targetAmount ?: BigDecimal.ZERO, account.currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (account.targetReached) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Goal reached",
                    tint = InvestmentGainColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
