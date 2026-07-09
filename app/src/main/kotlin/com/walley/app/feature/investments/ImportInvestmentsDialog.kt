package com.walley.app.feature.investments

import android.net.Uri
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.ImportRowOutcome
import com.walley.app.domain.model.ImportRowStatus

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
                        title = { Text("Import investments") },
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
                            onSelect = viewModel::selectAccountForImport
                        )
                        is ImportUiState.Preview -> PreviewContent(
                            outcomes = current.outcomes,
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
        TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) { Text("Close") }
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
        Button(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) { Text("Done") }
    }
}

@Composable
private fun SelectAccountContent(accounts: List<Account>, onSelect: (Account) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "This file doesn't say which account it's for. Which investment account should these events be added to?",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                Card(
                    onClick = { onSelect(account) },
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
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val toImportCount = outcomes.count { it.status is ImportRowStatus.ToImport }
    val duplicateCount = outcomes.count { it.status is ImportRowStatus.Duplicate }
    val rejectedCount = outcomes.count { it.status is ImportRowStatus.Rejected }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "$toImportCount to import · $duplicateCount already imported · $rejectedCount rejected",
                style = MaterialTheme.typography.titleMedium
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(outcomes, key = { it.rowNumber }) { outcome ->
                OutcomeRow(outcome)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onConfirm, enabled = toImportCount > 0, modifier = Modifier.weight(1f)) {
                Text(if (toImportCount > 0) "Import $toImportCount" else "Nothing to import")
            }
        }
    }
}

@Composable
private fun OutcomeRow(outcome: ImportRowOutcome) {
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
                val row = outcome.row
                Text(
                    if (row != null) {
                        "Row ${outcome.rowNumber} · ${row.type.label} ${row.ticker}"
                    } else {
                        "Row ${outcome.rowNumber}"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                StatusIndicator(outcome.status)
            }
            if (outcome.row != null) {
                Text(
                    "${outcome.row.accountName} · ${outcome.row.date} · ${outcome.row.quantity.toPlainString()} @ ${outcome.row.price.toPlainString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun StatusIndicator(status: ImportRowStatus) {
    when (status) {
        is ImportRowStatus.ToImport -> Icon(
            Icons.Filled.CheckCircle,
            contentDescription = "Will import",
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(20.dp)
        )
        is ImportRowStatus.Rejected -> Icon(
            Icons.Filled.Warning,
            contentDescription = "Rejected",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        is ImportRowStatus.Duplicate -> Text(
            "Already imported",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
