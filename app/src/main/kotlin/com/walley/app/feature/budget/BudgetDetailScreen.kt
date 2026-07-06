package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.SwipeToDeleteBox
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetSectionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetDetailViewModel = hiltViewModel()
) {
    val budgetWithItems by viewModel.budget.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()
    var itemForPaidDialog by remember { mutableStateOf<BudgetItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(budgetWithItems?.budget?.displayName ?: "Budget") },
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
        if (budget == null) {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {}
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BudgetSectionType.entries.forEach { section ->
                    val sectionItems = budget.items.filter { it.section == section }
                    if (sectionItems.isNotEmpty()) {
                        item(key = "header_${section.name}") {
                            Text(
                                section.label,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(sectionItems, key = { it.id }) { item ->
                            SwipeToDeleteBox(
                                onDelete = {
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
                            ) {
                                BudgetItemRow(
                                    item = item,
                                    accountName = item.accountId?.let { id -> accounts.find { it.id == id }?.name },
                                    onClick = { itemForPaidDialog = item }
                                )
                            }
                        }
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
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
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
}

@Composable
private fun BudgetItemRow(item: BudgetItem, accountName: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
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
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(formatMoney(item.amount, item.currency), style = MaterialTheme.typography.bodyLarge)
                    if (item.isCompleted) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Paid",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Paid", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                        }
                    } else if (item.paidAmount.signum() > 0) {
                        Text(
                            "Paid ${formatMoney(item.paidAmount, item.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Unpaid",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
