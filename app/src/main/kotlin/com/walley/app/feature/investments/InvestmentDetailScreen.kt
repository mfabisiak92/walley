package com.walley.app.feature.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.walley.app.core.ui.EquityStatusChip
import com.walley.app.core.ui.InvestmentCategoryChip
import com.walley.app.core.ui.SwipeToDeleteBox
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentTransaction
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.WatchedEquityWithNotes
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val TABS = listOf("Overview", "Events")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailScreen(
    onNavigateBack: () -> Unit,
    onOpenEquity: (Long) -> Unit,
    viewModel: InvestmentDetailViewModel = hiltViewModel()
) {
    val data by viewModel.investmentWithTransactions.collectAsStateWithLifecycle()
    val linkedAccount by viewModel.linkedAccount.collectAsStateWithLifecycle()
    val investmentsInAccount by viewModel.investmentsInAccount.collectAsStateWithLifecycle()
    val strategy by viewModel.strategy.collectAsStateWithLifecycle()
    val marketDataConfigured by viewModel.marketDataConfigured.collectAsStateWithLifecycle()
    val isRefreshingPrice by viewModel.isRefreshingPrice.collectAsStateWithLifecycle()
    val priceNotFoundReason by viewModel.priceNotFoundReason.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingTransactionType by remember { mutableStateOf<InvestmentTransactionType?>(null) }
    var editingTransaction by remember { mutableStateOf<InvestmentTransaction?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<InvestmentTransaction?>(null) }
    var showUpdatePriceDialog by remember { mutableStateOf(false) }
    var showDeleteInvestmentConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(priceNotFoundReason) {
        val reason = priceNotFoundReason
        if (reason != null) {
            val ticker = data?.investment?.ticker
            scope.launch { snackbarHostState.showSnackbar("${ticker ?: "Price"}: $reason") }
            viewModel.dismissPriceNotFound()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val investment = data?.investment
                    Text(investment?.let { listOfNotNull(it.name, it.ticker).joinToString(" · ") } ?: "Investment")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteInvestmentConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete investment")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { pendingTransactionType = InvestmentTransactionType.SELL },
                    icon = { Icon(Icons.Default.Remove, contentDescription = null) },
                    text = { Text("Sell") },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                ExtendedFloatingActionButton(
                    onClick = { pendingTransactionType = InvestmentTransactionType.BUY },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Buy") }
                )
            }
        }
    ) { innerPadding ->
        val current = data
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
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
            if (current == null) {
                return@Column
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> OverviewTab(
                        data = current,
                        strategy = strategy,
                        onClickCurrentPrice = { showUpdatePriceDialog = true },
                        onClickStrategy = onOpenEquity,
                        showRefreshPriceButton = marketDataConfigured,
                        isRefreshingPrice = isRefreshingPrice,
                        onRefreshPrice = viewModel::refreshPrice
                    )
                    else -> EventsTab(
                        data = current,
                        onClickTransaction = { editingTransaction = it },
                        onDeleteTransaction = { pendingDeleteTransaction = it }
                    )
                }
            }
        }
    }

    if (pendingTransactionType != null || editingTransaction != null) {
        val current = data
        val initial = editingTransaction
        if (current != null) {
            AddInvestmentTransactionDialog(
                data = current,
                account = linkedAccount,
                investmentsInAccount = investmentsInAccount,
                defaultType = pendingTransactionType ?: initial!!.type,
                initial = initial,
                onDismiss = {
                    pendingTransactionType = null
                    editingTransaction = null
                },
                onConfirm = { type, date, quantity, pricePerUnit, commission ->
                    if (initial != null) {
                        viewModel.updateTransaction(initial.id, type, date, quantity, pricePerUnit, commission)
                    } else {
                        viewModel.addTransaction(type, date, quantity, pricePerUnit, commission)
                    }
                    pendingTransactionType = null
                    editingTransaction = null
                }
            )
        }
    }

    pendingDeleteTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTransaction = null },
            title = { Text("Delete event?") },
            text = { Text("This will permanently delete this ${transaction.type.label.lowercase()} event. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction.id)
                        pendingDeleteTransaction = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTransaction = null }) { Text("Cancel") }
            }
        )
    }

    if (showUpdatePriceDialog) {
        data?.investment?.let { investment ->
            UpdateCurrentPriceDialog(
                investment = investment,
                onDismiss = { showUpdatePriceDialog = false },
                onSave = { currentPrice ->
                    viewModel.updateCurrentPrice(currentPrice)
                    showUpdatePriceDialog = false
                }
            )
        }
    }

    if (showDeleteInvestmentConfirm) {
        val name = data?.investment?.name ?: "this investment"
        AlertDialog(
            onDismissRequest = { showDeleteInvestmentConfirm = false },
            title = { Text("Delete investment?") },
            text = { Text("This will permanently delete \"$name\" and all its buy/sell events. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteInvestmentConfirm = false
                        viewModel.deleteInvestment(onNavigateBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteInvestmentConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun OverviewTab(
    data: InvestmentWithTransactions,
    strategy: WatchedEquityWithNotes?,
    onClickCurrentPrice: () -> Unit,
    onClickStrategy: (Long) -> Unit,
    showRefreshPriceButton: Boolean,
    isRefreshingPrice: Boolean,
    onRefreshPrice: () -> Unit
) {
    val investment = data.investment
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InvestmentCategoryChip(category = investment.category)
                strategy?.latestStatus?.let { status ->
                    EquityStatusChip(
                        status = status,
                        modifier = Modifier.clickable { onClickStrategy(strategy.equity.id) }
                    )
                }
            }
            Text(investment.ticker, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Current value",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(formatMoney(data.currentValue, investment.currency), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(6.dp))
                UnrealizedGainBadge(data.unrealizedGainLoss, data.unrealizedGainLossPercent, investment.currency)
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Quantity", data.quantity.toPlainString())
                    StatTile(
                        "Current price",
                        formatMoney(investment.currentPrice, investment.currency),
                        // A closed position's price never affects anything shown (value and gain/loss
                        // are both zero regardless of it), so editing/refreshing it here would be pointless.
                        onClick = if (data.quantity.signum() == 0) null else onClickCurrentPrice,
                        trailingIcon = if (showRefreshPriceButton && data.quantity.signum() != 0) {
                            {
                                if (isRefreshingPrice) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = "Refresh price",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        onTrailingIconClick = if (isRefreshingPrice) null else onRefreshPrice
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Avg cost", formatMoney(data.averageCost, investment.currency))
                    StatTile("Cost basis", formatMoney(data.costBasis, investment.currency))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("First purchase", data.firstPurchaseDate?.format(DATE_FORMATTER) ?: "—")
                    StatTile("Commission paid", formatMoney(data.totalCommissionPaid, investment.currency))
                }
                if (data.realizedGainLoss.signum() != 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    GainLossRow("Realized gain/loss", data.realizedGainLoss, null, investment.currency)
                }
            }
        }
    }
}

@Composable
private fun UnrealizedGainBadge(amount: BigDecimal, percent: BigDecimal?, currency: Currency) {
    val isGain = amount.signum() >= 0
    val color = if (isGain) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val sign = if (isGain) "+" else ""
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            if (isGain) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = "$sign${formatMoney(amount, currency)}" +
                (percent?.setScale(1, RoundingMode.HALF_UP)?.let { " ($sign${it.toPlainString()}%)" } ?: "") +
                " unrealized",
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun RowScope.StatTile(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onTrailingIconClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (trailingIcon != null) {
                        Box(
                            modifier = if (onTrailingIconClick != null) {
                                Modifier.clickable(onClick = onTrailingIconClick)
                            } else {
                                Modifier
                            }
                        ) { trailingIcon() }
                    }
                    if (onClick != null) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit $label",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun GainLossRow(label: String, amount: BigDecimal, percent: BigDecimal?, currency: Currency) {
    val isGain = amount.signum() >= 0
    val color = if (isGain) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val sign = if (isGain) "+" else ""
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "$sign${formatMoney(amount, currency)}" +
                (percent?.setScale(1, RoundingMode.HALF_UP)?.let { " ($sign${it.toPlainString()}%)" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun EventsTab(
    data: InvestmentWithTransactions,
    onClickTransaction: (InvestmentTransaction) -> Unit,
    onDeleteTransaction: (InvestmentTransaction) -> Unit
) {
    if (data.transactions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "No buy/sell events yet — tap Buy or Sell to add one.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val realizedByTransactionId = data.realizedGainLossByTransactionId
    val groupedByYear = data.transactions.groupBy { it.date.year }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedByYear.forEach { (year, transactionsInYear) ->
            item(key = "year-$year") {
                Text(
                    "$year · ${transactionsInYear.size} event${if (transactionsInYear.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(transactionsInYear, key = { it.id }) { transaction ->
                SwipeToDeleteBox(
                    onDelete = { onDeleteTransaction(transaction) },
                    dismissOnDelete = false
                ) {
                    TransactionRow(
                        transaction = transaction,
                        currency = data.investment.currency,
                        realizedGainLoss = realizedByTransactionId[transaction.id],
                        onClick = { onClickTransaction(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: InvestmentTransaction,
    currency: Currency,
    realizedGainLoss: BigDecimal?,
    onClick: () -> Unit
) {
    val isBuy = transaction.type == InvestmentTransactionType.BUY
    val typeColor = if (isBuy) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isBuy) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = typeColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(transaction.type.label, style = MaterialTheme.typography.bodyLarge)
                    Text(formatMoney(transaction.total, currency), style = MaterialTheme.typography.bodyLarge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        transaction.date.format(DATE_FORMATTER),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${transaction.quantity.toPlainString()} @ ${formatMoney(transaction.pricePerUnit, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (realizedGainLoss != null) {
                    val isGain = realizedGainLoss.signum() >= 0
                    val sign = if (isGain) "+" else ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Realized gain/loss",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$sign${formatMoney(realizedGainLoss, currency)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isGain) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        )
                    }
                } else if (transaction.commission.signum() > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Commission",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatMoney(transaction.commission, currency),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
