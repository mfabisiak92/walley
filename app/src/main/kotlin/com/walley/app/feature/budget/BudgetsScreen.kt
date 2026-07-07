package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.SwipeToDeleteBox
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.BudgetStatus
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    onCreateBudget: () -> Unit,
    onOpenBudget: (Long) -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val deleteBlockedMessage by viewModel.deleteBlockedMessage.collectAsStateWithLifecycle()
    var pendingDeleteBudget by remember { mutableStateOf<BudgetRowData?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { WalleyTopBar(onTitleClick = onNavigateHome) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateBudget) {
                Icon(Icons.Default.Add, contentDescription = "Create budget")
            }
        }
    ) { innerPadding ->
        if (budgets.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "No budgets yet — tap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(budgets, key = { it.budgetWithItems.budget.id }) { row ->
                    SwipeToDeleteBox(
                        onDelete = { pendingDeleteBudget = row },
                        dismissOnDelete = false
                    ) {
                        BudgetRow(
                            row = row,
                            onClick = { onOpenBudget(row.budgetWithItems.budget.id) }
                        )
                    }
                }
            }
        }
    }

    pendingDeleteBudget?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDeleteBudget = null },
            title = { Text("Delete budget?") },
            text = {
                Text(
                    "This will permanently delete \"${row.budgetWithItems.budget.displayName}\" and all its items. " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBudget(row.budgetWithItems.budget.id)
                        pendingDeleteBudget = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteBudget = null }) { Text("Cancel") }
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
private fun BudgetRow(row: BudgetRowData, onClick: () -> Unit) {
    val isCompleted = row.budgetWithItems.budget.status == BudgetStatus.COMPLETED

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isCompleted) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(row.budgetWithItems.budget.displayName, style = MaterialTheme.typography.titleMedium)
                if (isCompleted) {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Disposable income: " + (row.disposableIncome?.let { formatMoney(it, row.baseCurrency) } ?: "—"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Unallocated: " + (row.unallocated?.let { formatMoney(it, row.baseCurrency) }
                    ?: "exchange rate unavailable"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (row.progress == null) {
                Text(
                    "Progress unavailable — exchange rate missing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${formatMoney(row.progress.spent, row.baseCurrency)} / " +
                            formatMoney(row.progress.planned, row.baseCurrency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${row.progress.percent.setScale(0, RoundingMode.HALF_UP)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = {
                        row.progress.percent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}
