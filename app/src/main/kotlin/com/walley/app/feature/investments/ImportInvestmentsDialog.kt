package com.walley.app.feature.investments

import com.walley.app.R
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.FieldHint
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.ImportRowOutcome
import com.walley.app.domain.model.ImportRowStatus
import com.walley.app.domain.model.displayName

private val ImportGreen = Color(0xFF2E7D32)
private val ImportBlue = Color(0xFF42A5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportInvestmentsDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    viewModel: ImportInvestmentsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uri) { viewModel.load(uri) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.investments_import_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    when (val current = state) {
                        is ImportUiState.Loading -> LoadingContent("Reading file…")
                        is ImportUiState.Committing -> LoadingContent("Importing…")
                        is ImportUiState.Error -> ErrorContent(current.message, onDismiss)
                        is ImportUiState.Done -> DoneContent(current.importedCount, onDismiss)
                        is ImportUiState.SelectAccount -> SelectAccountContent(
                            accounts = current.accounts,
                            toggleKind = current.toggleKind,
                            onSelect = viewModel::selectAccountForImport
                        )
                        is ImportUiState.Preview -> PreviewContent(
                            outcomes = current.outcomes,
                            balanceReview = current.balanceReview,
                            onConfirm = viewModel::confirmImport,
                            onCancel = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ErrorContent(message: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.investments_action_close)) }
    }
}

@Composable
private fun DoneContent(importedCount: Int, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (importedCount == 1) "Imported 1 event" else "Imported $importedCount events",
            style = MaterialTheme.typography.headlineSmall
        )
        Button(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.investments_action_done)) }
    }
}

@Composable
private fun SelectAccountContent(
    accounts: List<Account>,
    toggleKind: ImportToggleKind,
    onSelect: (Account, Boolean) -> Unit
) {
    var toggleValue by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "This file doesn't say which account it's for. Which investment account should these events be added to?",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        if (toggleKind != ImportToggleKind.NONE) {
            val (label, hint) = when (toggleKind) {
                ImportToggleKind.INCLUDE_ACCOUNT_OPERATIONS -> "Include deposits, transfers & interest" to
                    "Adjusts this account's balance by each deposit/transfer/interest amount and by what's spent on buys (including commission), so the ending balance matches the statement even starting from zero. Only import a file once with this on."
                ImportToggleKind.IGNORE_ACCOUNT_BALANCE -> "Ignore account balance" to
                    "This file doesn't record deposits, so the account's balance can't be trusted to cover these buys. Turn this on to import every buy/sell regardless of the account's recorded cash — its balance is left untouched either way."
                ImportToggleKind.NONE -> "" to ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { toggleValue = !toggleValue },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    FieldHint(hint)
                }
                Switch(checked = toggleValue, onCheckedChange = { toggleValue = it })
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                Card(
                    onClick = { onSelect(account, toggleValue) },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(account.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            account.currency.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewContent(
    outcomes: List<ImportRowOutcome>,
    balanceReview: ImportBalanceReview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val tradeToImportCount = outcomes.count { it.status is ImportRowStatus.ToImport && it.cashOperation == null }
    val cashOperationToImportCount = outcomes.count { it.status is ImportRowStatus.ToImport && it.cashOperation != null }
    val toImportCount = tradeToImportCount + cashOperationToImportCount
    val duplicateCount = outcomes.count { it.status is ImportRowStatus.Duplicate }
    val rejectedCount = outcomes.count { it.status is ImportRowStatus.Rejected }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusCount(Icons.Filled.CheckCircle, ImportGreen, tradeToImportCount, "trades to import")
            StatusCount(Icons.Filled.Paid, ImportGreen, cashOperationToImportCount, "bank operations to import")
            StatusCount(Icons.Filled.DoneAll, ImportBlue, duplicateCount, "already imported")
            StatusCount(Icons.Filled.Warning, MaterialTheme.colorScheme.error, rejectedCount, "rejected")
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currencySymbolByAccountId = balanceReview.accountSummaries.associate { it.accountId to it.currencySymbol }
            if (balanceReview.accountSummaries.isNotEmpty()) {
                items(balanceReview.accountSummaries, key = { "summary_${it.accountId}" }) { summary ->
                    ImportAccountSummaryCard(summary)
                }
            }
            items(outcomes, key = { it.rowNumber }) { outcome ->
                val accountId = outcome.row?.accountId ?: outcome.cashOperation?.accountId
                OutcomeRow(
                    outcome = outcome,
                    balanceChange = balanceReview.rowChangesByRowNumber[outcome.rowNumber],
                    currencySymbol = currencySymbolByAccountId[accountId].orEmpty()
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.investments_action_cancel)) }
            Button(onClick = onConfirm, enabled = toImportCount > 0, modifier = Modifier.weight(1f)) {
                Text(if (toImportCount > 0) "Import $toImportCount" else "Nothing to import")
            }
        }
    }
}

@Composable
private fun StatusCount(icon: ImageVector, tint: Color, count: Int, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$count $description"
        }
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(count.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * One touched account's overall balance/net-balance impact for this import, plus how much cash it
 * added — the same before/after presentation [ReviewPriceUpdatesScreen] uses for price updates, so
 * the two review-style summaries look and read the same way across the app.
 */
@Composable
private fun ImportAccountSummaryCard(summary: ImportAccountSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                summary.accountName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            BalanceChangeBlock(
                label = stringResource(R.string.investments_label_account_balance),
                before = summary.beforeBalance,
                after = summary.afterBalance,
                change = summary.afterBalance - summary.beforeBalance,
                changePercent = null,
                currencySymbol = summary.currencySymbol,
                modifier = Modifier.padding(top = 8.dp)
            )
            BalanceChangeBlock(
                label = stringResource(R.string.investments_label_net_balance),
                before = summary.beforeNetBalance,
                after = summary.afterNetBalance,
                change = summary.afterNetBalance - summary.beforeNetBalance,
                changePercent = null,
                currencySymbol = summary.currencySymbol,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (summary.netCashAdded.signum() != 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.investments_label_cash_added),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatMoney(summary.netCashAdded, summary.currencySymbol),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun OutcomeRow(outcome: ImportRowOutcome, balanceChange: ImportRowBalanceChange?, currencySymbol: String) {
    val row = outcome.row
    val cashOperation = outcome.cashOperation
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when {
                        row != null -> "Row ${outcome.rowNumber} · ${row.type.displayName()} ${row.ticker}"
                        cashOperation != null -> "Row ${outcome.rowNumber} · ${cashOperation.description}"
                        else -> "Row ${outcome.rowNumber}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                StatusIndicator(outcome.status, isCashOperation = cashOperation != null)
            }
            if (row != null) {
                Text(
                    "${row.accountName} · ${row.date} · ${row.quantity.toPlainString()} @ ${row.price.toPlainString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (cashOperation != null) {
                Text(
                    "${cashOperation.accountName} · ${cashOperation.date} · ${cashOperation.amount.toPlainString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (balanceChange != null && balanceChange.balanceChanged) {
                BalanceChangeBlock(
                    label = stringResource(R.string.investments_label_account_balance),
                    before = balanceChange.beforeBalance,
                    after = balanceChange.afterBalance,
                    change = balanceChange.afterBalance - balanceChange.beforeBalance,
                    changePercent = null,
                    currencySymbol = currencySymbol,
                    compact = true,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            val reason = (outcome.status as? ImportRowStatus.Rejected)?.reason
            if (reason != null) {
                Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatusIndicator(status: ImportRowStatus, isCashOperation: Boolean) {
    when (status) {
        is ImportRowStatus.ToImport -> if (isCashOperation) {
            Icon(
                Icons.Filled.Paid,
                contentDescription = "Will import as a cash operation",
                tint = ImportGreen,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Will import",
                tint = ImportGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        is ImportRowStatus.Rejected -> Icon(
            Icons.Filled.Warning,
            contentDescription = "Rejected",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        is ImportRowStatus.Duplicate -> Icon(
            Icons.Filled.DoneAll,
            contentDescription = "Already imported",
            tint = ImportBlue,
            modifier = Modifier.size(20.dp)
        )
    }
}
