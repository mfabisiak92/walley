package com.walley.app.feature.investments

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.EquityStatusChip
import com.walley.app.core.ui.InvestmentCategoryChip
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.WatchedEquityWithNotes
import java.math.RoundingMode
import kotlinx.coroutines.launch

private val TABS = listOf("Portfolio", "Strategies")

@Composable
fun InvestmentsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    onOpenEquity: (Long) -> Unit,
    onOpenInvestment: (Long) -> Unit,
    onOpenUpdatePrices: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { TABS.size })
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
                TABS.forEachIndexed { index, label ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(label) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> PortfolioListPage(
                        onOpenInvestment = onOpenInvestment,
                        onOpenEquity = onOpenEquity,
                        onOpenUpdatePrices = onOpenUpdatePrices
                    )
                    else -> StrategiesListPage(onOpenEquity = onOpenEquity)
                }
            }
        }
    }
}

@Composable
private fun PortfolioListPage(
    onOpenInvestment: (Long) -> Unit,
    onOpenEquity: (Long) -> Unit,
    onOpenUpdatePrices: () -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel()
) {
    val investments by viewModel.investments.collectAsStateWithLifecycle()
    val investmentAccounts by viewModel.investmentAccounts.collectAsStateWithLifecycle()
    val strategiesByInvestmentId by viewModel.strategiesByInvestmentId.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<Investment?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showClosed by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImportUri = uri
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(onClick = { importLauncher.launch(arrayOf("text/*", "*/*")) }) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Import from file")
                }
                Spacer(modifier = Modifier.height(16.dp))
                SmallFloatingActionButton(onClick = onOpenUpdatePrices) {
                    Icon(Icons.Default.PriceCheck, contentDescription = "Update prices")
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add investment")
                }
            }
        }
    ) { innerPadding ->
        val closedCount = investments.count { it.quantity.signum() == 0 }
        val visibleInvestments = if (showClosed) investments else investments.filter { it.quantity.signum() != 0 }

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
            if (investments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No investments yet — tap + to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (visibleInvestments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "All positions are closed — tap \"Show closed\" above to see them.",
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
                    items(visibleInvestments, key = { it.investment.id }) { investment ->
                        InvestmentRow(
                            data = investment,
                            accountName = investment.investment.accountId?.let { id ->
                                investmentAccounts.find { it.id == id }?.name
                            },
                            strategy = strategiesByInvestmentId[investment.investment.id],
                            onClick = { onOpenInvestment(investment.investment.id) },
                            onLongClick = { editingInvestment = investment.investment },
                            onClickStrategy = { onOpenEquity(it) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddInvestmentDialog(
            investmentAccounts = investmentAccounts,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, ticker, externalTicker, category, purchaseDate, quantity, currency, price, currentPrice, accountId, commission ->
                viewModel.addInvestment(
                    name,
                    ticker,
                    externalTicker,
                    category,
                    purchaseDate,
                    quantity,
                    currency,
                    price,
                    currentPrice,
                    accountId,
                    commission
                )
                showAddDialog = false
            }
        )
    }

    editingInvestment?.let { investment ->
        EditInvestmentDialog(
            investment = investment,
            investmentAccounts = investmentAccounts,
            onDismiss = { editingInvestment = null },
            onSave = { name, ticker, externalTicker, category, accountId ->
                viewModel.updateInvestmentDetails(investment.id, name, ticker, externalTicker, category, accountId)
                editingInvestment = null
            }
        )
    }

    pendingImportUri?.let { uri ->
        ImportInvestmentsDialog(
            uri = uri,
            onDismiss = { pendingImportUri = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InvestmentRow(
    data: InvestmentWithTransactions,
    accountName: String?,
    strategy: WatchedEquityWithNotes?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClickStrategy: (Long) -> Unit
) {
    val investment = data.investment
    val isClosed = data.quantity.signum() == 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
        colors = if (isClosed) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = investment.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    formatMoney(data.currentValue, investment.currency),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = listOfNotNull(investment.ticker, accountName).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InvestmentCategoryChip(category = investment.category)
                    strategy?.latestStatus?.let { status ->
                        EquityStatusChip(
                            status = status,
                            modifier = Modifier.combinedClickable(onClick = { onClickStrategy(strategy.equity.id) })
                        )
                    }
                    if (isClosed) {
                        Text(
                            "Closed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${data.quantity.toPlainString()} @ ${formatMoney(investment.currentPrice, investment.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GainLossText(data)
            }
        }
    }
}

@Composable
private fun GainLossText(data: InvestmentWithTransactions) {
    val gainLoss = data.unrealizedGainLoss
    val isGain = gainLoss.signum() >= 0
    val color = if (isGain) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val sign = if (isGain) "+" else ""
    val percent = data.unrealizedGainLossPercent?.setScale(1, RoundingMode.HALF_UP)

    Text(
        text = "$sign${formatMoney(gainLoss, data.investment.currency)}" +
            (percent?.let { " ($sign${it.toPlainString()}%)" } ?: ""),
        style = MaterialTheme.typography.bodySmall,
        color = color
    )
}
