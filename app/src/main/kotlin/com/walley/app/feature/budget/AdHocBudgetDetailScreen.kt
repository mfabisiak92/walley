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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.BudgetItemIconBadge
import com.walley.app.core.ui.SwipeToCompleteBox
import com.walley.app.core.ui.paidProgressColor
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.AdHocCurrencyTotal
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdHocBudgetDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdHocBudgetDetailViewModel = hiltViewModel()
) {
    val budgetWithItems by viewModel.budget.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()
    var itemForPaidDialog by remember { mutableStateOf<AdHocBudgetItem?>(null) }
    var itemForEditDialog by remember { mutableStateOf<AdHocBudgetItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCompleteConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun deleteItemWithUndo(item: AdHocBudgetItem) {
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

    val isCompleted = budgetWithItems?.isCompleted == true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(budgetWithItems?.budget?.name ?: "Ad-hoc budget")
                        if (isCompleted) {
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
                    if (!isCompleted) {
                        IconButton(onClick = { showCompleteConfirm = true }) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Mark as completed")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete budget")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val budget = budgetWithItems
        val currentAccount = account
        if (budget == null || currentAccount == null) {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {}
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${budget.budget.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} – " +
                            budget.budget.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Default account: ${currentAccount.name} (${formatMoney(currentAccount.balance, currentAccount.currency)} left)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val currencyTotals = budget.totalsByCurrency(viewModel::currencyFor)
                    currencyTotals.forEach { total ->
                        AdHocProgressSection(
                            total = total,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(budget.items, key = { it.id }) { item ->
                        val itemAccount = viewModel.accountFor(item) ?: currentAccount
                        val row: @Composable () -> Unit = {
                            AdHocBudgetItemRow(
                                item = item,
                                currency = itemAccount.currency,
                                accountName = itemAccount.name.takeIf { itemAccount.id != currentAccount.id },
                                onClick = if (isCompleted) null else ({ itemForPaidDialog = item }),
                                onLongClick = if (isCompleted) null else ({ itemForEditDialog = item })
                            )
                        }
                        if (!isCompleted && !item.isCompleted) {
                            SwipeToCompleteBox(onComplete = {
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
                            }) { row() }
                        } else {
                            row()
                        }
                    }
                }
            }
        }
    }

    itemForPaidDialog?.let { item ->
        viewModel.accountFor(item)?.let { itemAccount ->
            MarkAdHocItemPaidDialog(
                item = item,
                currency = itemAccount.currency,
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
    }

    itemForEditDialog?.let { item ->
        viewModel.accountFor(item)?.let { itemAccount ->
            EditAdHocItemAmountDialog(
                item = item,
                accountBalance = itemAccount.balance,
                currency = itemAccount.currency,
                onDismiss = { itemForEditDialog = null },
                onSave = { amount, icon ->
                    viewModel.updateItemAmount(item.id, amount)
                    if (icon != item.icon) {
                        viewModel.updateItemIcon(item.id, icon)
                    }
                    itemForEditDialog = null
                },
                onDelete = {
                    itemForEditDialog = null
                    deleteItemWithUndo(item)
                }
            )
        }
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
        val isEarly = budgetWithItems?.budget?.endDate?.let { LocalDate.now().isBefore(it) } == true
        AlertDialog(
            onDismissRequest = { showCompleteConfirm = false },
            title = { Text("Mark budget as completed?") },
            text = {
                Text(
                    (if (isEarly) {
                        "This budget's end date hasn't passed yet — anything you haven't paid will just stop " +
                            "being tracked. "
                    } else {
                        ""
                    }) +
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
private fun AdHocProgressSection(total: AdHocCurrencyTotal, modifier: Modifier = Modifier) {
    val percent = if (total.planned.signum() == 0) {
        BigDecimal.ZERO
    } else {
        (total.paid.divide(total.planned, 6, RoundingMode.HALF_UP) * BigDecimal(100))
            .coerceIn(BigDecimal.ZERO, BigDecimal(100))
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${formatMoney(total.paid, total.currency)} / ${formatMoney(total.planned, total.currency)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${percent.setScale(0, RoundingMode.HALF_UP)}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        LinearProgressIndicator(
            progress = {
                percent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdHocBudgetItemRow(
    item: AdHocBudgetItem,
    currency: Currency,
    accountName: String?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?
) {
    val progress = if (item.amount.signum() > 0) {
        item.paidAmount.divide(item.amount, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else {
        0f
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
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BudgetItemIconBadge(icon = item.icon, size = 28.dp)
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (accountName != null) {
                            Text(
                                "From $accountName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Text(
                    "${formatMoney(item.paidAmount, currency)} / ${formatMoney(item.amount, currency)}",
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
}
