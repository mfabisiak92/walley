package com.walley.app.feature.budget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.BudgetItemIconBadge
import com.walley.app.core.ui.PieChartCard
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.PieSlice
import com.walley.app.core.ui.SwipeToCompleteBox
import com.walley.app.core.ui.paidProgressColor
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    onNavigateBack: () -> Unit,
    onCloneBudget: (Long) -> Unit,
    viewModel: BudgetDetailViewModel = hiltViewModel()
) {
    val budgetWithItems by viewModel.budget.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val (baseCurrency, rates) = viewModel.baseCurrencyRates.collectAsStateWithLifecycle().value
    val categoryTargets by viewModel.categoryTargets.collectAsStateWithLifecycle()
    val projectedNetWorth by viewModel.projectedNetWorth.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()
    var itemForPaidDialog by remember { mutableStateOf<BudgetItem?>(null) }
    var itemForEditDialog by remember { mutableStateOf<BudgetItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCompleteConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val status = budgetWithItems?.budget?.status
    val isEditable = status == BudgetStatus.ACTIVE
    val pagerState = rememberPagerState(pageCount = { BudgetDetailTab.entries.size })
    val tabScope = rememberCoroutineScope()

    fun deleteItemWithUndo(item: BudgetItem) {
        viewModel.deleteItem(item.id)
        scope.launch {
            val dismissJob = launch {
                delay(5_000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = "\"${item.name}\" deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Indefinite
            )
            dismissJob.cancel()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreItem(item)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(budgetWithItems?.budget?.displayName ?: "Budget")
                        if (status == BudgetStatus.COMPLETED) {
                            Text(
                                "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (status == BudgetStatus.ACTIVE) {
                        IconButton(onClick = { showCompleteConfirm = true }) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Mark as completed")
                        }
                    }
                    IconButton(onClick = { budgetWithItems?.budget?.id?.let(onCloneBudget) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Clone to another month")
                    }
                    if (status != BudgetStatus.COMPLETED) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete budget")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val budget = budgetWithItems
        if (budget == null) {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {}
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                    BudgetDetailTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { tabScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(tab.label) }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (val tab = BudgetDetailTab.entries[page]) {
                        BudgetDetailTab.SUMMARY -> SummaryTabContent(
                            items = budget.items,
                            baseCurrency = baseCurrency,
                            rates = rates,
                            projectedNetWorth = projectedNetWorth,
                            applyAccountEffects = budget.budget.applyAccountEffects,
                            isEditable = isEditable,
                            onToggleApplyAccountEffects = viewModel::updateApplyAccountEffects
                        )
                        else -> SectionTabContent(
                            allItems = budget.items,
                            tab = tab,
                            baseCurrency = baseCurrency,
                            rates = rates,
                            accounts = accounts,
                            categoryTargets = categoryTargets,
                            isEditable = isEditable,
                            onItemClick = { itemForPaidDialog = it },
                            onItemLongClick = { itemForEditDialog = it },
                            onSwipeMarkPaid = { item ->
                                val previousPaidAmount = item.paidAmount
                                viewModel.markPaid(item.id)
                                scope.launch {
                                    val dismissJob = launch {
                                        delay(5_000)
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                    }
                                    val result = snackbarHostState.showSnackbar(
                                        message = "\"${item.name}\" marked as paid",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Indefinite
                                    )
                                    dismissJob.cancel()
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.markPartiallyPaid(item.id, previousPaidAmount)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    itemForPaidDialog?.let { item ->
        MarkPaidDialog(
            item = item,
            onDismiss = { itemForPaidDialog = null },
            onMarkFullyPaid = {
                viewModel.markPaid(item.id)
                itemForPaidDialog = null
            },
            onMarkPartiallyPaid = { amount ->
                viewModel.markPartiallyPaid(item.id, amount)
                itemForPaidDialog = null
            }
        )
    }

    itemForEditDialog?.let { item ->
        EditItemAmountDialog(
            item = item,
            accounts = accounts,
            onDismiss = { itemForEditDialog = null },
            onSave = { amount, icon, accountId ->
                viewModel.updateItemAmount(item.id, amount)
                if (icon != item.icon) {
                    viewModel.updateItemIcon(item.id, icon)
                }
                if (accountId != item.accountId) {
                    viewModel.updateItemAccount(item.id, accountId)
                }
                itemForEditDialog = null
            },
            onDelete = {
                itemForEditDialog = null
                deleteItemWithUndo(item)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete budget?") },
            text = { Text("This will permanently delete this budget and all its items. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteBudget(onNavigateBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    deleteBlockedMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteBlockedMessage,
            title = { Text("Can't delete budget") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissDeleteBlockedMessage) { Text("OK") }
            }
        )
    }

    if (showCompleteConfirm) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirm = false },
            title = { Text("Mark budget as completed?") },
            text = {
                Text(
                    "This is a one-way change. Once completed, this budget and its items become read-only " +
                        "— nothing can be paid, deleted, or otherwise changed, and the budget itself can no " +
                        "longer be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompleteConfirm = false
                        viewModel.markCompleted()
                    }
                ) { Text("Mark completed") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionTabContent(
    allItems: List<BudgetItem>,
    tab: BudgetDetailTab,
    baseCurrency: Currency,
    rates: ExchangeRates?,
    accounts: List<Account>,
    categoryTargets: Map<BudgetSectionType, BigDecimal?>,
    isEditable: Boolean,
    onItemClick: (BudgetItem) -> Unit,
    onItemLongClick: (BudgetItem) -> Unit,
    onSwipeMarkPaid: (BudgetItem) -> Unit
) {
    val items = allItems.filter { it.section in tab.sections }
    val progress = budgetProgress(items, tab.sections, baseCurrency, rates)
    val targetSection = tab.sections.singleOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        ProgressSummaryHeader(progress = progress, currency = baseCurrency, modifier = Modifier.padding(16.dp))
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tab.sections.forEach { section ->
                val sectionItems = items.filter { it.section == section }
                if (sectionItems.isNotEmpty()) {
                    if (tab.sections.size > 1) {
                        item(key = "header_${section.name}") {
                            Text(
                                section.label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                    }
                    items(sectionItems, key = { it.id }) { budgetItem ->
                        val row: @Composable () -> Unit = {
                            BudgetItemRow(
                                item = budgetItem,
                                accountName = budgetItem.accountId?.let { id -> accounts.find { it.id == id }?.name },
                                onClick = if (isEditable) ({ onItemClick(budgetItem) }) else null,
                                onLongClick = if (isEditable) ({ onItemLongClick(budgetItem) }) else null
                            )
                        }
                        if (isEditable && !budgetItem.isCompleted) {
                            SwipeToCompleteBox(onComplete = { onSwipeMarkPaid(budgetItem) }) { row() }
                        } else {
                            row()
                        }
                    }
                }
            }
        }
        if (targetSection != null) {
            val target = categoryTargets[targetSection]
            if (target != null) {
                val disposable = disposableIncome(allItems, baseCurrency, rates)
                val sectionAmount = sectionTotal(allItems, targetSection, baseCurrency, rates)
                val actualPercent = if (disposable != null && sectionAmount != null && disposable.signum() != 0) {
                    (sectionAmount.divide(disposable, 4, RoundingMode.HALF_UP) * BigDecimal(100))
                } else {
                    null
                }
                HorizontalDivider()
                CategoryTargetIndicator(
                    section = targetSection,
                    actualPercent = actualPercent,
                    targetPercent = target,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryTabContent(
    items: List<BudgetItem>,
    baseCurrency: Currency,
    rates: ExchangeRates?,
    projectedNetWorth: BigDecimal?,
    applyAccountEffects: Boolean,
    isEditable: Boolean,
    onToggleApplyAccountEffects: (Boolean) -> Unit
) {
    val overallProgress = budgetProgress(items, SPENDING_SECTIONS, baseCurrency, rates)
    val disposable = disposableIncome(items, baseCurrency, rates)
    val unallocated = unallocatedAmount(items, baseCurrency, rates)
    val fixed = sectionTotal(items, BudgetSectionType.FIXED_COSTS, baseCurrency, rates)
    val other = sectionTotal(items, BudgetSectionType.OTHER_COSTS, baseCurrency, rates)
    val savings = sectionTotal(items, BudgetSectionType.SAVINGS, baseCurrency, rates)
    val investments = sectionTotal(items, BudgetSectionType.INVESTMENTS, baseCurrency, rates)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProgressSummaryHeader(progress = overallProgress, currency = baseCurrency)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Draw from linked accounts", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "When off, paying items here won't move money in any account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = applyAccountEffects, onCheckedChange = onToggleApplyAccountEffects, enabled = isEditable)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SummaryRow("Projected net worth (end of month)", projectedNetWorth, baseCurrency)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SummaryRow("Disposable income", disposable, baseCurrency)
        SummaryRow("Unallocated", unallocated, baseCurrency)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SummaryRow("Fixed costs", fixed, baseCurrency)
        SummaryRow("Other costs", other, baseCurrency)
        SummaryRow("Savings", savings, baseCurrency)
        SummaryRow("Investments", investments, baseCurrency)

        if (disposable != null && disposable.signum() > 0 && fixed != null && other != null &&
            savings != null && investments != null
        ) {
            val labels = listOf("Fixed costs", "Other costs", "Savings", "Investments")
            val amounts = listOf(fixed, other, savings, investments)
            val slices = labels.indices.mapNotNull { index ->
                val amount = amounts[index]
                if (amount.signum() <= 0) return@mapNotNull null
                val percent = amount.divide(disposable, 4, RoundingMode.HALF_UP) * BigDecimal(100)
                PieSlice(
                    label = "${labels[index]} · ${percent.setScale(1, RoundingMode.HALF_UP)}% · " +
                        formatMoney(amount, baseCurrency),
                    percent = percent.toFloat(),
                    color = PieChartColors[index % PieChartColors.size]
                )
            }
            if (slices.isNotEmpty()) {
                PieChartCard(title = "Budget allocation", slices = slices)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: BigDecimal?, currency: Currency) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            amount?.let { formatMoney(it, currency) } ?: "—",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ProgressSummaryHeader(progress: BudgetProgress?, currency: Currency, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (progress == null) {
            Text(
                "Exchange rate unavailable — progress can't be computed right now.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${formatMoney(progress.spent, currency)} / ${formatMoney(progress.planned, currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${progress.percent.setScale(0, RoundingMode.HALF_UP)}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            LinearProgressIndicator(
                progress = {
                    progress.percent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BudgetItemRow(
    item: BudgetItem,
    accountName: String?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?
) {
    val progress = if (item.amount.signum() > 0) {
        item.paidAmount.divide(item.amount, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BudgetItemIconBadge(icon = item.icon, size = 28.dp)
                    Column {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium)
                        val subtitleParts = listOfNotNull(
                            accountName,
                            when {
                                item.paymentDayIsLastOfMonth -> "Last day of month"
                                item.paymentDay != null -> "Day ${item.paymentDay}"
                                else -> null
                            }
                        )
                        if (subtitleParts.isNotEmpty()) {
                            Text(
                                subtitleParts.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    "${formatMoney(item.paidAmount, item.currency)} / ${formatMoney(item.amount, item.currency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = paidProgressColor(progress)
            )
        }
    }

    val cardModifier = if (onClick != null || onLongClick != null) {
        Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = { onClick?.invoke() })
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) { content() }
}
