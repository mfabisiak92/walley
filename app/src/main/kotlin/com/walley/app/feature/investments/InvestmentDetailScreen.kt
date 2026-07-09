package com.walley.app.feature.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.InvestmentCategoryChip
import com.walley.app.core.ui.SwipeToDeleteBox
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentTransaction
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
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
    viewModel: InvestmentDetailViewModel = hiltViewModel()
) {
    val data by viewModel.investmentWithTransactions.collectAsStateWithLifecycle()
    val linkedAccount by viewModel.linkedAccount.collectAsStateWithLifecycle()
    val investmentsInAccount by viewModel.investmentsInAccount.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()
    var pendingTransactionType by remember { mutableStateOf<InvestmentTransactionType?>(null) }
    var editingTransaction by remember { mutableStateOf<InvestmentTransaction?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<InvestmentTransaction?>(null) }
    var showUpdatePriceDialog by remember { mutableStateOf(false) }
    var showDeleteInvestmentConfirm by remember { mutableStateOf(false) }

    Scaffold(
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
                    0 -> OverviewTab(data = current, onClickCurrentPrice = { showUpdatePriceDialog = true })
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
                onConfirm = { type, date, quantity, pricePerUnit ->
                    if (initial != null) {
                        viewModel.updateTransaction(initial.id, type, date, quantity, pricePerUnit)
                    } else {
                        viewModel.addTransaction(type, date, quantity, pricePerUnit)
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
private fun OverviewTab(data: InvestmentWithTransactions, onClickCurrentPrice: () -> Unit) {
    val investment = data.investment
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InvestmentCategoryChip(category = investment.category)
        }
        StatCard {
            StatRow("Current value", formatMoney(data.currentValue, investment.currency))
            StatRow("Quantity held", data.quantity.toPlainString())
            StatRow(
                "Current price",
                formatMoney(investment.currentPrice, investment.currency),
                onClick = onClickCurrentPrice
            )
        }
        StatCard {
            StatRow("Average cost", formatMoney(data.averageCost, investment.currency))
            StatRow("Cost basis", formatMoney(data.costBasis, investment.currency))
            StatRow(
                "First purchase",
                data.firstPurchaseDate?.format(DATE_FORMATTER) ?: "—"
            )
        }
        StatCard {
            GainLossRow("Unrealized gain/loss", data.unrealizedGainLoss, data.unrealizedGainLossPercent, investment.currency)
            GainLossRow("Realized gain/loss", data.realizedGainLoss, null, investment.currency)
        }
    }
}

@Composable
private fun StatCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun StatRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onClick != null) {
            TextButton(onClick = onClick) { Text(value) }
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium)
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(data.transactions, key = { it.id }) { transaction ->
            SwipeToDeleteBox(
                onDelete = { onDeleteTransaction(transaction) },
                dismissOnDelete = false
            ) {
                TransactionRow(
                    transaction = transaction,
                    currency = data.investment.currency,
                    onClick = { onClickTransaction(transaction) }
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: InvestmentTransaction, currency: Currency, onClick: () -> Unit) {
    val isBuy = transaction.type == InvestmentTransactionType.BUY
    val typeColor = if (isBuy) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(transaction.type.label, style = MaterialTheme.typography.titleMedium, color = typeColor)
                Text(formatMoney(transaction.total, currency), style = MaterialTheme.typography.titleMedium)
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
        }
    }
}
