package com.walley.app.feature.investments

import com.walley.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.walley.app.domain.model.displayName
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlinx.coroutines.launch

private val TABS = listOf("Overview", "Events")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy")
private val AXIS_MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM ''yy")

/**
 * Scaffold's innerPadding never insets for a `floatingActionButton` (only topBar/bottomBar/system
 * bars), so a tall enough scrollable tab can end up with its last content sitting right under the
 * stacked Sell/Buy FABs. Both scrollable tabs below add this as extra bottom content padding instead —
 * sized generously past the two 56dp `ExtendedFloatingActionButton`s plus their 12dp gap and margin.
 */
private val FAB_STACK_CLEARANCE = 160.dp

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
    val dividendsSummary by viewModel.dividendsSummary.collectAsStateWithLifecycle()
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
                    text = { Text(stringResource(R.string.investments_action_sell)) },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
                ExtendedFloatingActionButton(
                    onClick = { pendingTransactionType = InvestmentTransactionType.BUY },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.investments_action_buy)) }
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
                        dividendsSummary = dividendsSummary,
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
            title = { Text(stringResource(R.string.investments_delete_event_title)) },
            text = { Text(stringResource(R.string.investments_delete_event_message, transaction.type.displayName().lowercase())) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(transaction.id)
                        pendingDeleteTransaction = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.investments_action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTransaction = null }) { Text(stringResource(R.string.investments_action_cancel)) }
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
                },
                onRevertToPrevious = { viewModel.revertToPreviousPrice() }
            )
        }
    }

    if (showDeleteInvestmentConfirm) {
        val name = data?.investment?.name ?: "this investment"
        AlertDialog(
            onDismissRequest = { showDeleteInvestmentConfirm = false },
            title = { Text(stringResource(R.string.investments_delete_investment_title)) },
            text = { Text(stringResource(R.string.investments_delete_investment_message, name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteInvestmentConfirm = false
                        viewModel.deleteInvestment(onNavigateBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.investments_action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteInvestmentConfirm = false }) { Text(stringResource(R.string.investments_action_cancel)) }
            }
        )
    }
}

@Composable
private fun OverviewTab(
    data: InvestmentWithTransactions,
    strategy: WatchedEquityWithNotes?,
    dividendsSummary: DividendsSummary,
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
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + FAB_STACK_CLEARANCE),
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
                if (dividendsSummary.gross.signum() != 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("Dividends", formatMoney(dividendsSummary.gross, investment.currency))
                        StatTile("Net dividends", formatMoney(dividendsSummary.net, investment.currency))
                    }
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
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + FAB_STACK_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "price-chart") {
            InvestmentPriceChart(
                transactions = data.transactions,
                currentPrice = data.investment.currentPrice,
                currency = data.investment.currency,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
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

private enum class ChartPointKind { BUY, SELL, CURRENT }

private data class ChartDataPoint(val date: LocalDate, val price: BigDecimal, val kind: ChartPointKind)

private data class AxisDateTick(val date: LocalDate, val label: String)

/** A selectable time window for the price chart; [days] is how far back from today to show, null meaning show everything. */
private enum class ChartRange(val label: String, val days: Long?) {
    ONE_MONTH("1M", 30L),
    SIX_MONTHS("6M", 182L),
    ONE_YEAR("1Y", 365L),
    FIVE_YEARS("5Y", 365L * 5),
    ALL("All", null)
}

/**
 * Line chart of every buy/sell event's own recorded price plus today's current price, connected in
 * chronological order — there's no separate market-price history, so this is exactly the price the
 * user paid/received at each event. Points are evenly spaced by order (not by elapsed time) so they
 * stay legible and tappable even when several events happened close together; once there are more
 * than fit comfortably, the chart grows wider than the screen and scrolls horizontally instead of
 * squeezing points together. Tapping a point shows its date and price below the chart. The x-axis
 * ticks at a calendar cadence (month/quarter/half-year/year) chosen from the total span, and the
 * y-axis shows rounded price ticks on the left; both can be hidden as gridlines via the toggle.
 */
@Composable
private fun InvestmentPriceChart(
    transactions: List<InvestmentTransaction>,
    currentPrice: BigDecimal,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val allPoints = remember(transactions, currentPrice) {
        transactions.sortedWith(compareBy({ it.date }, { it.id }))
            .map { ChartDataPoint(it.date, it.pricePerUnit, if (it.type == InvestmentTransactionType.BUY) ChartPointKind.BUY else ChartPointKind.SELL) } +
            ChartDataPoint(LocalDate.now(), currentPrice, ChartPointKind.CURRENT)
    }
    var selectedRange by remember { mutableStateOf(ChartRange.ALL) }
    // The nominal window the range represents — NOT just the span of whatever data happens to fall in
    // it. A short history under a long range (e.g. two months of trades under "1Y") must still show a
    // full year on the axis, with the real events clustered wherever they actually fall in it.
    val rangeEnd = allPoints.last().date
    val rangeStart = if (selectedRange == ChartRange.ALL) allPoints.first().date else rangeEnd.minusDays(selectedRange.days!!)
    val points = remember(allPoints, selectedRange, rangeStart) {
        if (selectedRange == ChartRange.ALL) {
            allPoints
        } else {
            allPoints.filter { !it.date.isBefore(rangeStart) }.ifEmpty { listOf(allPoints.last()) }
        }
    }
    val hasNoEventsInRange = selectedRange != ChartRange.ALL && points.none { it.kind != ChartPointKind.CURRENT }
    val buyColor = Color(0xFF2E7D32)
    val sellColor = MaterialTheme.colorScheme.error
    val currentColor = MaterialTheme.colorScheme.tertiary
    val lineColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    var gridVisible by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Price history", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { gridVisible = !gridVisible }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (gridVisible) Icons.Filled.GridOn else Icons.Filled.GridOff,
                        contentDescription = if (gridVisible) "Hide grid" else "Show grid",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartRange.values().forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            if (hasNoEventsInRange) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "No events in this period",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val density = LocalDensity.current
            val pointRadiusPx = with(density) { 14.dp.toPx() }
            val markerTextSizePx = with(density) { 15.sp.toPx() }
            val axisTextSizePx = with(density) { 11.sp.toPx() }
            val strokeWidthPx = with(density) { 2.dp.toPx() }
            val gridStrokeWidthPx = with(density) { 1.dp.toPx() }
            val axisLabelHeightPx = with(density) { 20.dp.toPx() }
            val verticalMarginPx = pointRadiusPx + with(density) { 6.dp.toPx() }
            val edgePaddingDp = 20.dp
            val edgePaddingPx = with(density) { edgePaddingDp.toPx() }
            val minGapPx = with(density) { 56.dp.toPx() }
            val yAxisWidthDp = 44.dp
            val chartHeightDp = 200.dp

            val minPriceD = points.minOf { it.price }.toDouble()
            val maxPriceD = points.maxOf { it.price }.toDouble()
            val priceAxisTicks = remember(minPriceD, maxPriceD) { niceAxisTicks(minPriceD, maxPriceD) }
            val scaleMin = priceAxisTicks.first()
            val scaleMax = priceAxisTicks.last()
            val scaleRange = (scaleMax - scaleMin).toFloat()

            val dateAxisTicks = remember(rangeStart, rangeEnd) { buildDateAxisTicks(rangeStart, rangeEnd) }
            val axisMeasurePaint = remember(axisTextSizePx) {
                android.graphics.Paint().apply { textSize = axisTextSizePx; isAntiAlias = true }
            }

            val totalRangeDays = ChronoUnit.DAYS.between(rangeStart, rangeEnd).toFloat().coerceAtLeast(1f)

            // Where a date would fall along a plot of [plotWidthPx] wide if time flowed at a constant
            // rate — i.e. proportional to elapsed days, not to how many events happen to exist nearby.
            fun idealXForDate(date: LocalDate, plotWidthPx: Float): Float {
                val dayOffset = ChronoUnit.DAYS.between(rangeStart, date).toFloat().coerceIn(0f, totalRangeDays)
                return edgePaddingPx + plotWidthPx * dayOffset / totalRangeDays
            }

            fun yForValue(value: Double, canvasHeightPx: Float): Float {
                val plotHeight = canvasHeightPx - verticalMarginPx * 2 - axisLabelHeightPx
                return if (scaleRange <= 0f) {
                    verticalMarginPx + plotHeight / 2f
                } else {
                    verticalMarginPx + plotHeight * (1f - ((value - scaleMin) / scaleRange).toFloat())
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .width(yAxisWidthDp)
                        .height(chartHeightDp)
                ) {
                    priceAxisTicks.forEach { tick ->
                        val y = yForValue(tick, size.height)
                        drawContext.canvas.nativeCanvas.drawText(
                            formatAxisNumber(tick),
                            size.width - with(density) { 6.dp.toPx() },
                            y + axisTextSizePx / 3f,
                            android.graphics.Paint().apply {
                                color = labelColor.toArgb()
                                textSize = axisTextSizePx
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    // "All" squeezes every point into one screen instead of spacing them out, so it never scrolls.
                    val maxWidthPx = with(density) { maxWidth.toPx() }
                    val allowScroll = selectedRange != ChartRange.ALL

                    // Points are placed proportional to elapsed time within [rangeStart, rangeEnd] (not
                    // evenly by order), so a short history inside a long range shows real empty space —
                    // then nudged apart left-to-right so none sit closer than minGapPx (never overlapping,
                    // even if several events share a date). "All" must never scroll, so if that nudging
                    // would overflow the screen it's compressed (scaled) back down to fit instead of
                    // growing the canvas; every other range is allowed to grow and scroll instead.
                    val (pointX, contentWidthPx, xScale) = remember(points, rangeStart, rangeEnd, maxWidthPx, allowScroll) {
                        val plotWidthPx = maxWidthPx - edgePaddingPx * 2
                        val ideal = points.map { idealXForDate(it.date, plotWidthPx) }
                        val adjusted = FloatArray(ideal.size)
                        if (ideal.isNotEmpty()) {
                            adjusted[0] = ideal[0]
                            for (i in 1 until ideal.size) {
                                adjusted[i] = if (ideal[i] - adjusted[i - 1] < minGapPx) adjusted[i - 1] + minGapPx else ideal[i]
                            }
                        }
                        val neededRightEdge = if (adjusted.isEmpty()) maxWidthPx else adjusted.last() + edgePaddingPx
                        if (neededRightEdge <= maxWidthPx || allowScroll) {
                            Triple(adjusted.toList(), maxOf(maxWidthPx, neededRightEdge), 1f)
                        } else {
                            val scale = ((maxWidthPx - edgePaddingPx * 2) / (adjusted.last() - edgePaddingPx).coerceAtLeast(1f)).coerceAtMost(1f)
                            Triple(adjusted.map { edgePaddingPx + (it - edgePaddingPx) * scale }, maxWidthPx, scale)
                        }
                    }
                    val contentWidth = with(density) { contentWidthPx.toDp() }
                    val scrollState = rememberScrollState()

                    fun xForDate(date: LocalDate): Float {
                        val raw = idealXForDate(date, maxWidthPx - edgePaddingPx * 2)
                        return edgePaddingPx + (raw - edgePaddingPx) * xScale
                    }

                    // Open scrolled to the most recent end (today/current price) — scrolling left then reveals older history.
                    LaunchedEffect(points, contentWidth) {
                        scrollState.scrollTo(Int.MAX_VALUE)
                    }

                    val visibleDateAxisTicks = remember(dateAxisTicks, maxWidthPx, xScale) {
                        val xPositions = dateAxisTicks.map { xForDate(it.date) }
                        val minSpacingPx = (dateAxisTicks.maxOfOrNull { axisMeasurePaint.measureText(it.label) } ?: 0f) +
                            with(density) { 12.dp.toPx() }
                        thinAxisTicksToFit(dateAxisTicks, xPositions, minSpacingPx)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .width(contentWidth)
                                .height(chartHeightDp)
                                .pointerInput(points) {
                                    detectTapGestures(
                                        onTap = { tapOffset ->
                                            val canvasHeightPx = size.height.toFloat()
                                            val tapRadiusPx = pointRadiusPx * 2.2f
                                            var closestIndex: Int? = null
                                            var closestDistanceSq = Float.MAX_VALUE
                                            points.forEachIndexed { index, point ->
                                                val px = pointX[index]
                                                val py = yForValue(point.price.toDouble(), canvasHeightPx)
                                                val dx = px - tapOffset.x
                                                val dy = py - tapOffset.y
                                                val distanceSq = dx * dx + dy * dy
                                                if (distanceSq < closestDistanceSq) {
                                                    closestDistanceSq = distanceSq
                                                    closestIndex = index
                                                }
                                            }
                                            selectedIndex = closestIndex?.takeIf { closestDistanceSq <= tapRadiusPx * tapRadiusPx }
                                                ?.let { if (selectedIndex == it) null else it }
                                        }
                                    )
                                }
                        ) {
                            val canvasWidthPx = size.width
                            val canvasHeightPx = size.height
                            val plotBottomPx = canvasHeightPx - axisLabelHeightPx
                            val offsets = points.mapIndexed { index, point ->
                                Offset(pointX[index], yForValue(point.price.toDouble(), canvasHeightPx))
                            }

                            if (gridVisible) {
                                priceAxisTicks.forEach { tick ->
                                    val y = yForValue(tick, canvasHeightPx)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidthPx, y),
                                        strokeWidth = gridStrokeWidthPx
                                    )
                                }
                                visibleDateAxisTicks.forEach { tick ->
                                    val x = xForDate(tick.date)
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(x, 0f),
                                        end = Offset(x, plotBottomPx),
                                        strokeWidth = gridStrokeWidthPx
                                    )
                                }
                            }

                            for (i in 0 until offsets.size - 1) {
                                drawLine(color = lineColor, start = offsets[i], end = offsets[i + 1], strokeWidth = strokeWidthPx)
                            }

                            visibleDateAxisTicks.forEach { tick ->
                                drawContext.canvas.nativeCanvas.drawText(
                                    tick.label,
                                    xForDate(tick.date),
                                    canvasHeightPx - axisLabelHeightPx / 2f + axisTextSizePx / 3f,
                                    android.graphics.Paint().apply {
                                        color = labelColor.toArgb()
                                        textSize = axisTextSizePx
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        isAntiAlias = true
                                    }
                                )
                            }

                            offsets.forEachIndexed { index, point ->
                                val color = when (points[index].kind) {
                                    ChartPointKind.BUY -> buyColor
                                    ChartPointKind.SELL -> sellColor
                                    ChartPointKind.CURRENT -> currentColor
                                }
                                if (selectedIndex == index) {
                                    drawCircle(
                                        color = lineColor,
                                        radius = pointRadiusPx + strokeWidthPx * 2,
                                        center = point,
                                        style = Stroke(width = strokeWidthPx)
                                    )
                                }
                                drawCircle(color = color, radius = pointRadiusPx, center = point)
                                val symbol = when (points[index].kind) {
                                    ChartPointKind.BUY -> "+"
                                    ChartPointKind.SELL -> "-"
                                    ChartPointKind.CURRENT -> "?"
                                }
                                val symbolPaint = android.graphics.Paint().apply {
                                    this.color = Color.White.toArgb()
                                    textSize = markerTextSizePx
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                    isFakeBoldText = true
                                }
                                val textY = point.y - (symbolPaint.ascent() + symbolPaint.descent()) / 2f
                                drawContext.canvas.nativeCanvas.drawText(symbol, point.x, textY, symbolPaint)
                            }
                        }
                    }
                }
            }

            val selected = selectedIndex?.let { points.getOrNull(it) }
            if (selected != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val kindLabel = when (selected.kind) {
                        ChartPointKind.BUY -> "Buy"
                        ChartPointKind.SELL -> "Sell"
                        ChartPointKind.CURRENT -> "Current price"
                    }
                    Text("$kindLabel · ${selected.date.format(DATE_FORMATTER)}", style = MaterialTheme.typography.labelMedium)
                    Text(formatMoney(selected.price, currency), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Drops candidate ticks that would render closer than [minSpacingPx] to the previously kept one, so
 * labels never overlap regardless of how many calendar ticks the cadence produced for the chart width.
 * The final tick is always kept (swapping out its too-close predecessor if needed) since it anchors
 * the most recent end of the timeline.
 */
private fun thinAxisTicksToFit(ticks: List<AxisDateTick>, xPositions: List<Float>, minSpacingPx: Float): List<AxisDateTick> {
    if (ticks.size <= 1) return ticks
    val keptIndices = mutableListOf(0)
    for (i in 1 until ticks.size) {
        if (xPositions[i] - xPositions[keptIndices.last()] >= minSpacingPx) keptIndices += i
    }
    val lastIndex = ticks.lastIndex
    if (keptIndices.last() != lastIndex) {
        // The last tick always wins over whichever kept tick it's too close to, even if that's the very first one.
        if (xPositions[lastIndex] - xPositions[keptIndices.last()] < minSpacingPx) {
            keptIndices.removeAt(keptIndices.lastIndex)
        }
        keptIndices += lastIndex
    }
    return keptIndices.map { ticks[it] }
}

/** Builds calendar-spaced x-axis ticks between [first] and [last], picking a cadence from the total span. */
private fun buildDateAxisTicks(first: LocalDate, last: LocalDate): List<AxisDateTick> {
    if (!first.isBefore(last)) return listOf(AxisDateTick(first, first.format(AXIS_MONTH_FORMATTER)))
    val spanDays = ChronoUnit.DAYS.between(first, last)
    val intervalMonths = when {
        spanDays < 365 -> 1
        spanDays < 3 * 365 -> 3
        spanDays < 6 * 365 -> 6
        else -> 12
    }
    val ticks = mutableListOf<AxisDateTick>()
    var cursor = YearMonth.from(first)
    val lastMonth = YearMonth.from(last)
    var isFirst = true
    while (!cursor.isAfter(lastMonth)) {
        val tickDate = if (isFirst) first else cursor.atDay(1)
        ticks += AxisDateTick(tickDate, if (intervalMonths >= 12) cursor.year.toString() else tickDate.format(AXIS_MONTH_FORMATTER))
        isFirst = false
        cursor = cursor.plusMonths(intervalMonths.toLong())
    }
    if (ticks.last().date != last) {
        ticks += AxisDateTick(last, if (intervalMonths >= 12) last.year.toString() else last.format(AXIS_MONTH_FORMATTER))
    }
    return ticks
}

/** Classic Heckbert "nice numbers" step, rounded to a 1/2/5/10 multiple of its order of magnitude. */
private fun niceNumber(range: Double, round: Boolean): Double {
    if (range <= 0.0) return 1.0
    val exponent = floor(log10(range))
    val fraction = range / 10.0.pow(exponent)
    val niceFraction = if (round) {
        when {
            fraction < 1.5 -> 1.0
            fraction < 3.0 -> 2.0
            fraction < 7.0 -> 5.0
            else -> 10.0
        }
    } else {
        when {
            fraction <= 1.0 -> 1.0
            fraction <= 2.0 -> 2.0
            fraction <= 5.0 -> 5.0
            else -> 10.0
        }
    }
    return niceFraction * 10.0.pow(exponent)
}

/** Round-number y-axis ticks (e.g. 10, 20, 50, 100) spanning at least [min]..[max]. */
private fun niceAxisTicks(min: Double, max: Double, targetTickCount: Int = 5): List<Double> {
    if (min == max) return listOf(min - 1.0, min, min + 1.0)
    val range = niceNumber(max - min, false)
    val step = niceNumber(range / (targetTickCount - 1).coerceAtLeast(1), true)
    val niceMin = floor(min / step) * step
    val niceMax = ceil(max / step) * step
    val ticks = mutableListOf<Double>()
    var v = niceMin
    while (v <= niceMax + step / 2) {
        ticks += v
        v += step
    }
    return ticks
}

/** Compact axis label for a round number, e.g. 1500.0 -> "1.5k", 10_000_000.0 -> "10M". */
private fun formatAxisNumber(value: Double): String {
    val absValue = abs(value)
    val (divisor, suffix) = when {
        absValue >= 1_000_000_000.0 -> 1_000_000_000.0 to "B"
        absValue >= 1_000_000.0 -> 1_000_000.0 to "M"
        absValue >= 1_000.0 -> 1_000.0 to "k"
        else -> 1.0 to ""
    }
    val scaled = value / divisor
    val text = if (scaled == floor(scaled)) scaled.toLong().toString() else String.format(Locale.US, "%.1f", scaled)
    return text + suffix
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
                    Text(transaction.type.displayName(), style = MaterialTheme.typography.bodyLarge)
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
