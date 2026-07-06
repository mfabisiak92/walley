package com.walley.app.feature.investments

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.Investment

@Composable
fun InvestmentsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    viewModel: InvestmentsViewModel = hiltViewModel()
) {
    val investments by viewModel.investments.collectAsStateWithLifecycle()
    val investmentAccounts by viewModel.investmentAccounts.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<Investment?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { WalleyTopBar(onTitleClick = onNavigateHome) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add investment")
            }
        }
    ) { innerPadding ->
        if (investments.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(investments, key = { it.id }) { investment ->
                    InvestmentRow(
                        investment = investment,
                        accountName = investment.accountId?.let { id ->
                            investmentAccounts.find { it.id == id }?.name
                        },
                        onClick = { editingInvestment = investment }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddInvestmentDialog(
            investmentAccounts = investmentAccounts,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, ticker, quantity, currency, price, accountId ->
                viewModel.addInvestment(name, ticker, quantity, currency, price, accountId)
                showAddDialog = false
            }
        )
    }

    editingInvestment?.let { investment ->
        EditInvestmentDialog(
            investment = investment,
            investmentAccounts = investmentAccounts,
            onDismiss = { editingInvestment = null },
            onSave = { name, ticker, quantity, price, accountId ->
                viewModel.updateInvestment(investment.id, name, ticker, quantity, price, accountId)
                editingInvestment = null
            },
            onDelete = {
                viewModel.deleteInvestment(investment.id)
                editingInvestment = null
            }
        )
    }
}

@Composable
private fun InvestmentRow(
    investment: Investment,
    accountName: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(investment.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = accountName?.let { "${investment.ticker} · $it" } ?: investment.ticker,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatMoney(investment.value, investment.currency),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "${investment.quantity.toPlainString()} @ ${formatMoney(investment.price, investment.currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
