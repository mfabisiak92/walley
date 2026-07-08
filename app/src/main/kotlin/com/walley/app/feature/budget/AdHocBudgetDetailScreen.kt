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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.BudgetItemIconBadge
import com.walley.app.core.ui.SwipeToCompleteBox
import com.walley.app.core.ui.paidProgressColor
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
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
    var itemForPaidDialog by remember { mutableStateOf<AdHocBudgetItem?>(null) }
    var itemForEditDialog by remember { mutableStateOf<AdHocBudgetItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(budgetWithItems?.budget?.name ?: "Ad-hoc budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete budget")
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
                        "Paying from: ${currentAccount.name} (${formatMoney(currentAccount.balance, currentAccount.currency)} left)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val planned = budget.totalPlanned
                    val paid = budget.totalPaid
                    val percent = if (planned.signum() == 0) {
                        BigDecimal.ZERO
                    } else {
                        (paid.divide(planned, 6, RoundingMode.HALF_UP) * BigDecimal(100))
                            .coerceIn(BigDecimal.ZERO, BigDecimal(100))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${formatMoney(paid, currentAccount.currency)} / ${formatMoney(planned, currentAccount.currency)}",
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
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(budget.items, key = { it.id }) { item ->
                        val row: @Composable () -> Unit = {
                            AdHocBudgetItemRow(
                                item = item,
                                currency = currentAccount.currency,
                                onClick = { itemForPaidDialog = item },
                                onLongClick = { itemForEditDialog = item }
                            )
                        }
                        if (!item.isCompleted) {
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
        account?.let { currentAccount ->
            MarkAdHocItemPaidDialog(
                item = item,
                currency = currentAccount.currency,
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
        account?.let { currentAccount ->
            EditAdHocItemAmountDialog(
                item = item,
                accountBalance = currentAccount.balance,
                currency = currentAccount.currency,
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdHocBudgetItemRow(
    item: AdHocBudgetItem,
    currency: Currency,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val progress = if (item.amount.signum() > 0) {
        item.paidAmount.divide(item.amount, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BudgetItemIconBadge(icon = item.icon, size = 28.dp)
                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
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
