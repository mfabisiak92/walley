package com.walley.app.feature.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.InvestmentGainColor
import com.walley.app.core.ui.InvestmentNeutralColor
import com.walley.app.core.ui.RemovableChip
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.core.ui.paidProgressColor
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.SortDirection
import com.walley.app.domain.model.displayName
import com.walley.app.feature.budget.convertToCurrency
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
                            Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.accounts_cd_sort_and_filter))
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
                        text = { Text(group.displayName()) }
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
            val label = if (filterState.status == AccountStatusFilter.CLOSED) {
                stringResource(R.string.accounts_closed)
            } else {
                stringResource(R.string.accounts_filter_status_all)
            }
            RemovableChip(label = label, onRemove = onStatusReset)
        }
        filterState.currencies.forEach { currency ->
            RemovableChip(label = currency.name, onRemove = { onCurrencyToggled(currency) })
        }
        filterState.types.forEach { type ->
            RemovableChip(label = type.displayName(), onRemove = { onTypeToggled(type) })
        }
        if (filterState.kind != AccountKindFilter.ALL) {
            val label = if (filterState.kind == AccountKindFilter.REAL) {
                stringResource(R.string.accounts_real)
            } else {
                stringResource(R.string.accounts_virtual)
            }
            RemovableChip(label = label, onRemove = onKindReset)
        }
        TextButton(onClick = onResetAll) { Text(stringResource(R.string.accounts_reset_all)) }
    }
}

@Composable
private fun sortChipLabel(sort: AccountsSortState): String {
    val fieldLabel = when (sort.field) {
        AccountSortField.NAME -> stringResource(R.string.accounts_name)
        AccountSortField.BALANCE -> stringResource(R.string.accounts_balance)
        AccountSortField.DATE_ADDED -> stringResource(R.string.accounts_date_added)
        AccountSortField.DEFAULT_FIRST -> return stringResource(R.string.accounts_sort_chip_default_first)
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
    val tabLabel = group.displayName()
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
    val transferBlockedMessage by viewModel.transferBlockedMessage.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    val visibleAccounts = sortAccounts(filterAccounts(filteredAccounts, userFilterState), sortState, baseCurrency, exchangeRates)
    // Not tied to this tab's own account types — a transfer can cross tabs (e.g. Checking → Saving), so
    // this only needs *some* pair of open accounts to exist anywhere, not specifically within [group].
    val canTransfer = accounts.count { !it.isClosed } >= 2

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (canTransfer) {
                    SmallFloatingActionButton(onClick = { showTransferDialog = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.accounts_transfer_money_title))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (filteredAccounts.isNotEmpty()) {
                    SmallFloatingActionButton(onClick = { onOpenUpdateBalances(group) }) {
                        Icon(Icons.Default.EditNote, contentDescription = stringResource(R.string.accounts_update_balances))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.accounts_add_account))
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
                        stringResource(R.string.accounts_empty_state_no_type_accounts, tabLabel),
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
                        stringResource(R.string.accounts_empty_state_no_match_filters),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { viewModel.resetFilters(group) }) {
                        Text(stringResource(R.string.accounts_reset_filters))
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
                                // physically sits inside one of the accounts shown here — see AccountsTotalHeader.
                                val virtualSavingsAccounts = accounts.filter { it.isVirtual && it.type == AccountType.SAVING && !it.isClosed }
                                val availableAccounts = checkingCashAccounts + virtualSavingsAccounts
                                // Driven by which currencies actually have a checking/cash account, rather than
                                // dropping zero totals — so Available still shows a 0 line for a currency whose
                                // envelopes exactly cancel out its checking/cash total.
                                val currencies = checkingCashAccounts.map { it.currency }.distinct()
                                AccountsTotalHeader(
                                    captionLabel = stringResource(R.string.accounts_available),
                                    entries = currencies.map { currency ->
                                        val total = checkingCashAccounts.filter { it.currency == currency }.sumOf { it.balance }
                                        val available = availableAccounts.filter { it.currency == currency }
                                            .sumOf { account -> if (account.isVirtual) -account.balance else account.balance }
                                        CurrencyHeroTotal(currency, hero = total, caption = available)
                                    },
                                    baseCurrency = baseCurrency,
                                    exchangeRates = exchangeRates,
                                    showCaptionPerCurrency = true
                                )
                            }
                            AccountBalanceGroup.SAVINGS -> {
                                // Hero is Standard + Virtual combined — Virtual envelopes are separate money on
                                // this tab (unlike on Cash & Checking, where they're earmarked out of a host
                                // account), so there's no double-counting in adding them together here.
                                val virtualSavingsAccounts = activeAccounts.filter { it.isVirtual }
                                val currencies = activeAccounts.map { it.currency }.distinct()
                                AccountsTotalHeader(
                                    captionLabel = stringResource(R.string.accounts_virtual),
                                    entries = currencies.map { currency ->
                                        val combined = activeAccounts.filter { it.currency == currency }.sumOf { it.balance }
                                        val virtual = virtualSavingsAccounts.filter { it.currency == currency }.sumOf { it.balance }
                                        CurrencyHeroTotal(currency, hero = combined, caption = virtual)
                                    },
                                    baseCurrency = baseCurrency,
                                    exchangeRates = exchangeRates,
                                    showCaptionPerCurrency = false
                                )
                            }
                            AccountBalanceGroup.INVESTMENTS -> {
                                val investmentAccounts = activeAccounts.filterNot { it.isVirtual }.filter { it.currency == baseCurrency }
                                if (investmentAccounts.isNotEmpty()) {
                                    val lossColor = MaterialTheme.colorScheme.error
                                    // Per-account after-tax profit summed, rather than summing pre-tax gain/loss and
                                    // re-deriving one flat tax on top — accounts can carry different tax rates, so
                                    // taxing the aggregate would misstate accounts that are actually tax-free.
                                    val netProfitAfterTax = investmentAccounts.sumOf { it.investmentNetProfit ?: it.investmentGainLoss }
                                    val costBasis = investmentAccounts.sumOf { it.investmentCostBasis }
                                    val gainLossPercent = costBasis.takeIf { it.signum() > 0 }
                                        ?.let { basis -> netProfitAfterTax.divide(basis, 4, RoundingMode.HALF_UP) * BigDecimal(100) }
                                    val netColor = when {
                                        netProfitAfterTax.signum() > 0 -> InvestmentGainColor
                                        netProfitAfterTax.signum() < 0 -> lossColor
                                        else -> InvestmentNeutralColor
                                    }
                                    InvestmentsTotalHeader(
                                        total = investmentAccounts.sumOf { it.balance },
                                        net = investmentAccounts.sumOf { it.netWorthValue },
                                        cash = investmentAccounts.sumOf { it.uninvestedCash },
                                        netGain = netProfitAfterTax,
                                        netGainPercent = gainLossPercent,
                                        currency = baseCurrency,
                                        netColor = netColor,
                                        cashLabel = stringResource(R.string.accounts_label_cash_short)
                                    )
                                }
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

    if (showTransferDialog) {
        TransferMoneyDialog(
            accounts = accounts,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromAccountId, toAccountId, amount ->
                viewModel.transferBetweenAccounts(fromAccountId, toAccountId, amount)
                showTransferDialog = false
            }
        )
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
            title = { Text(stringResource(R.string.accounts_cant_delete_account_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteBlockedMessage) { Text(stringResource(R.string.accounts_ok)) }
            }
        )
    }

    closeBlockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissCloseBlockedMessage,
            title = { Text(stringResource(R.string.accounts_cant_close_account_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissCloseBlockedMessage) { Text(stringResource(R.string.accounts_ok)) }
            }
        )
    }

    transferBlockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissTransferBlockedMessage,
            title = { Text(stringResource(R.string.accounts_transfer_failed_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissTransferBlockedMessage) { Text(stringResource(R.string.accounts_ok)) }
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
        val isInvestment = account.type == AccountType.INVESTMENT
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (isInvestment) {
                InvestmentAccountBody(account = account, onReopen = onReopen)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccountTypeIcon(type = account.type)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                account.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = listOfNotNull(
                                // Redundant within the Savings tab, which is already single-type — only
                                // Cash & Checking mixes both types, so the label still disambiguates there.
                                account.type.displayName().takeIf { account.type == AccountType.CHECKING || account.type == AccountType.CASH },
                                account.targetDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                stringResource(R.string.accounts_virtual).takeIf { account.isVirtual },
                                stringResource(R.string.accounts_closed).takeIf { account.isClosed }
                            ).joinToString(" · ")
                            if (subtitle.isNotEmpty()) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (account.isClosed) {
                        OutlinedButton(
                            onClick = onReopen,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.accounts_reopen), style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        // Star sits inline with the balance rather than stacked below it — the
                        // previous layout's main source of extra height for no extra information.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatMoney(account.balance, account.currency), style = MaterialTheme.typography.titleSmall)
                            if (canBeDefault) {
                                DefaultAccountButton(isDefault = account.isDefault, onClick = onSetDefault)
                            }
                        }
                    }
                }
                val progressPercent = account.targetProgressPercent
                if (!account.isClosed) {
                    if (progressPercent != null) {
                        // Paid-this-month rides on the same row as the progress bar's own percent/target
                        // text instead of a separate divider+row above it — that extra block was the
                        // main source of this row's height beyond the plain name/balance line.
                        SavingsGoalProgress(account = account, progressPercent = progressPercent, paidThisMonth = paidThisMonth)
                    } else if (paidThisMonth != null) {
                        // No goal set on this account, so there's no progress row to attach to — falls
                        // back to its own row same as before.
                        MonthPaidRow(amount = paidThisMonth, currency = account.currency)
                    }
                }
            }
        }
    }
}

/**
 * Investments tab's row layout — icon-badged name top-left (no tax-rate subtitle; the account already
 * lives on a single-type tab) with the plain account balance as the hero figure top-right, one compact
 * line pairing Net balance and Uninvested cash below that, and the unrealized profit/loss (with its %
 * return on cost basis, in brackets) on its own line at the bottom. Invested amount isn't shown
 * separately here — it's just balance minus uninvested cash, so nothing is lost by leaving it off the
 * list; it's still visible on the account's own detail screen one tap away. Net balance and the profit
 * line share [profitColor] — green for a gain, [MaterialTheme.colorScheme.error] for a loss,
 * [InvestmentNeutralColor] when flat — same three-way split [InvestmentSummaryCard] uses.
 */
@Composable
private fun InvestmentAccountBody(account: Account, onReopen: () -> Unit) {
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountTypeIcon(type = account.type)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(account.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (account.isClosed) {
                    Text(stringResource(R.string.accounts_closed), style = MaterialTheme.typography.labelSmall, color = labelColor)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (account.isClosed) {
            OutlinedButton(
                onClick = onReopen,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.accounts_reopen), style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Text(formatMoney(account.balance, account.currency), style = MaterialTheme.typography.titleSmall)
        }
    }
    if (!account.isClosed) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.accounts_label_cash_short)} ${formatMoney(account.uninvestedCash, account.currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatMoney(account.netWorthValue, account.currency),
                style = MaterialTheme.typography.bodySmall,
                color = profitColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            val percentText = profitPercent?.let { " (${it.setScale(2, RoundingMode.HALF_UP)}%)" }.orEmpty()
            Text(
                "${formatMoney(profit, account.currency)}$percentText",
                style = MaterialTheme.typography.bodyMedium,
                color = profitColor
            )
        }
    }
}

/** Small round icon badge identifying an account's type at a glance in the compact list row. */
@Composable
private fun AccountTypeIcon(type: AccountType) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = accountTypeIcon(type),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CHECKING -> Icons.Filled.AccountBalance
    AccountType.CASH -> Icons.Filled.Payments
    AccountType.SAVING -> Icons.Filled.Savings
    AccountType.INVESTMENT -> Icons.Filled.TrendingUp
}

/** Marks/unmarks this [AccountType.CHECKING]/[AccountType.CASH] account as the one used by default — a concept that only makes sense for everyday spending accounts, not savings goals or investment accounts. */
@Composable
private fun DefaultAccountButton(isDefault: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        if (isDefault) {
            Icon(
                Icons.Filled.Star,
                contentDescription = stringResource(R.string.accounts_cd_default_account),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                Icons.Outlined.StarOutline,
                contentDescription = stringResource(R.string.accounts_cd_set_as_default_account),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One currency's raw (unconverted) line feeding [AccountsTotalHeader] — a hero figure plus the smaller caption figure. */
private data class CurrencyHeroTotal(
    val currency: Currency,
    val hero: BigDecimal,
    val caption: BigDecimal
)

/**
 * Summary header shared by Cash & Checking and Savings (Investments uses its own [InvestmentsTotalHeader]
 * — its top-left/top-right grid doesn't fit this hero+caption shape): a single hero figure — the number
 * that matters most on this tab, converted to [baseCurrency] and summed across every currency present —
 * with a caption line of secondary detail underneath it, on the screen's own tonal surface so it follows
 * the light/dark theme automatically (unlike the old hardcoded light-blue tile the Home screen's
 * "Checking, cash & savings" card still uses). A currency [exchangeRates] has no rate for contributes
 * nothing to the hero/caption totals rather than distorting them with an unconverted raw amount.
 *
 * When accounts span more than just [baseCurrency], a chevron next to the hero expands a per-currency
 * breakdown — [baseCurrency]'s own accounts first (the collapsed hero is a sum across every currency,
 * so this is the only place to see how much of it is actually baseCurrency money rather than something
 * else converted in), then each other currency, each amount followed by "≈" and its [baseCurrency]
 * equivalent wherever a rate is available.
 *
 * - Cash & Checking ([showCaptionPerCurrency] set): Total as the hero, Available balance (Total minus
 *   any virtual Savings envelope earmarked from it) as the caption. Expanded rows use
 *   [CurrencyBreakdownRow] — three lines per currency: the ISO code, Total (with its "≈" conversion),
 *   and Available (with its own "≈" conversion) — so both figures are traceable per currency, not just
 *   the hero.
 * - Savings: Standard + Virtual combined as the hero, Virtual alone as the caption — since this tab's
 *   list mixes both, the caption is the only way to tell from the header how much of that combined
 *   total is parked in a virtual sub-account rather than a real, separate one. Expanded rows use
 *   [CompactCurrencyRow] — one line per currency, since there's no second figure to break out.
 */
@Composable
private fun AccountsTotalHeader(
    captionLabel: String,
    entries: List<CurrencyHeroTotal>,
    baseCurrency: Currency,
    exchangeRates: ExchangeRates?,
    showCaptionPerCurrency: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val otherCurrencyEntries = entries.filter { it.currency != baseCurrency }
    // Base currency's own entry goes first — the collapsed hero is a sum across every currency, so
    // without this row there's no way to tell how much of it actually sits in baseCurrency accounts
    // versus how much is EUR/NOK/etc. converted in.
    val expandedEntries = listOfNotNull(entries.find { it.currency == baseCurrency }) + otherCurrencyEntries

    val heroTotal = entries.sumOf { convertToCurrency(it.hero, it.currency, baseCurrency, exchangeRates) ?: BigDecimal.ZERO }
    val captionTotal = entries.sumOf { convertToCurrency(it.caption, it.currency, baseCurrency, exchangeRates) ?: BigDecimal.ZERO }

    // No card/tile — sits directly on the screen background. Same horizontal/vertical padding an
    // AccountRow's own Card applies to its content, so the hero and caption text line up with the
    // account rows' text below despite there being no boundary to align to.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                formatMoney(heroTotal, baseCurrency),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (otherCurrencyEntries.isNotEmpty()) {
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(
                            if (expanded) R.string.accounts_cd_collapse_currencies else R.string.accounts_cd_expand_currencies
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }
        }
        Text(
            "$captionLabel: ${formatMoney(captionTotal, baseCurrency)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (expanded && otherCurrencyEntries.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                expandedEntries.forEach { entry ->
                    if (showCaptionPerCurrency) {
                        CurrencyBreakdownRow(
                            entry = entry,
                            baseCurrency = baseCurrency,
                            captionLabel = captionLabel,
                            exchangeRates = exchangeRates
                        )
                    } else {
                        CompactCurrencyRow(entry = entry, baseCurrency = baseCurrency, exchangeRates = exchangeRates)
                    }
                }
            }
        }
    }
}

/**
 * Cash & Checking's row in [AccountsTotalHeader]'s expanded breakdown — three lines: the currency's
 * ISO code, Total in that currency (with, whenever a rate is available, "≈" followed by its
 * [baseCurrency] equivalent), then the same treatment for [captionLabel] (Available). [entry]'s own
 * currency being [baseCurrency] needs no "≈" side on either line — it's already what the collapsed
 * hero is denominated in.
 */
@Composable
private fun CurrencyBreakdownRow(entry: CurrencyHeroTotal, baseCurrency: Currency, captionLabel: String, exchangeRates: ExchangeRates?) {
    val isBase = entry.currency == baseCurrency
    val rateUnavailable = stringResource(R.string.accounts_exchange_rate_unavailable)

    fun line(amount: BigDecimal): String {
        val native = formatMoney(amount, entry.currency)
        if (isBase) return native
        val converted = convertToCurrency(amount, entry.currency, baseCurrency, exchangeRates)
        return if (converted != null) "$native ≈ ${formatMoney(converted, baseCurrency)}" else "$native — $rateUnavailable"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(entry.currency.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(line(entry.hero), style = MaterialTheme.typography.bodySmall)
        Text(
            "$captionLabel ${line(entry.caption)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Savings' row in [AccountsTotalHeader]'s expanded breakdown — one line: the currency's ISO code, its
 * native amount, and, whenever a rate is available, "≈" followed by its [baseCurrency] equivalent.
 * [entry]'s own currency being [baseCurrency] needs no "≈" side — it's already what the collapsed hero
 * is denominated in.
 */
@Composable
private fun CompactCurrencyRow(entry: CurrencyHeroTotal, baseCurrency: Currency, exchangeRates: ExchangeRates?) {
    val nativeAmount = "${entry.currency.name} ${formatMoney(entry.hero, entry.currency)}"
    val line = if (entry.currency == baseCurrency) {
        nativeAmount
    } else {
        val converted = convertToCurrency(entry.hero, entry.currency, baseCurrency, exchangeRates)
        val convertedText = if (converted != null) {
            "≈ ${formatMoney(converted, baseCurrency)}"
        } else {
            stringResource(R.string.accounts_exchange_rate_unavailable)
        }
        "$nativeAmount $convertedText"
    }
    Text(line, style = MaterialTheme.typography.bodySmall)
}

/**
 * Investments tab's summary header — a 2×2 grid mirroring the shape of the Home screen's own
 * Investments card: Total sits unlabeled in the top-left as the plain hero figure, Net (after tax, also
 * unlabeled) in the top-right, Uninvested cash underneath Total, and the tax-aware gain/loss amount —
 * with its return on cost basis in brackets, same figure as each account row — underneath Net at the
 * same size as Net itself. Net and the gain/loss line both share [netColor] — tinted green/red/blue by
 * whether there's a gain or loss, same as the account rows below.
 */
@Composable
private fun InvestmentsTotalHeader(
    total: BigDecimal,
    net: BigDecimal,
    cash: BigDecimal,
    netGain: BigDecimal,
    netGainPercent: BigDecimal?,
    currency: Currency,
    netColor: Color,
    cashLabel: String
) {
    // No card/tile — sits directly on the screen background, same as AccountsTotalHeader.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(formatMoney(total, currency), style = MaterialTheme.typography.titleLarge)
            Text(formatMoney(net, currency), style = MaterialTheme.typography.titleMedium, color = netColor)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "$cashLabel: ${formatMoney(cash, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val percentText = netGainPercent?.let { " (${it.setScale(2, RoundingMode.HALF_UP)}%)" }.orEmpty()
            Text(
                "${formatMoney(netGain, currency)}$percentText",
                style = MaterialTheme.typography.titleMedium,
                color = netColor
            )
        }
    }
}

/**
 * "Paid this month" — how much has been contributed so far to this Saving account's linked SAVINGS
 * budget item(s) in the current month's budget. Fallback for an account with no target set (so there's
 * no [SavingsGoalProgress] row to attach to) — a dedicated row, set off by a divider, label on the left
 * and the amount on the right. Whenever a target *is* set, this same information instead rides on
 * [SavingsGoalProgress]'s own row, right beside its percent/target text.
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
            stringResource(R.string.accounts_paid_this_month),
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
private fun SavingsGoalProgress(account: Account, progressPercent: BigDecimal, paidThisMonth: BigDecimal?) {
    val progressFraction = progressPercent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    Column(modifier = Modifier.padding(top = 8.dp)) {
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = paidProgressColor(progressFraction)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.accounts_progress_percent_of,
                    progressPercent.setScale(0, RoundingMode.HALF_UP),
                    formatMoney(account.targetAmount ?: BigDecimal.ZERO, account.currency)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (paidThisMonth != null) {
                    Text(
                        text = "${stringResource(R.string.accounts_paid_this_month)}: ${formatMoney(paidThisMonth, account.currency)}",
                        // Same bodySmall size as the percent/target text on the left, so the two halves
                        // of this row read as one line rather than a big label next to a small one.
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (account.targetReached) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.accounts_cd_goal_reached),
                        tint = InvestmentGainColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
