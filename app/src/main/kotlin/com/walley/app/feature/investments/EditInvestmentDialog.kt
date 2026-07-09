package com.walley.app.feature.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentCategory

/** Edits an investment's identity/metadata. Position size and cost basis are managed via its buy/sell events instead. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditInvestmentDialog(
    investment: Investment,
    investmentAccounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        ticker: String,
        category: InvestmentCategory,
        accountId: Long
    ) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(investment.name) }
    var ticker by remember { mutableStateOf(investment.ticker) }
    var category by remember { mutableStateOf(investment.category) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var accountId by remember { mutableStateOf(investment.accountId) }
    var accountTouched by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Currency is fixed after creation, so valid targets are same-currency investment accounts.
    val selectableAccounts = investmentAccounts.filter { it.currency == investment.currency }
    val selectedAccount = selectableAccounts.find { it.id == accountId }

    val isValid = name.isNotBlank() && ticker.isNotBlank() && selectedAccount != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit investment") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectableAccounts.isEmpty()) {
                    Text(
                        "No investment account in ${investment.currency.name} exists yet. " +
                            "Create one from the Accounts screen to keep this investment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it },
                    label = { Text("Ticker") },
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        InvestmentCategory.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    category = option
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                InvestmentAccountDropdown(
                    accounts = selectableAccounts,
                    selectedAccountId = accountId,
                    onAccountSelected = {
                        accountId = it
                        accountTouched = true
                    },
                    isError = accountTouched && selectedAccount == null
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete investment")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name.trim(), ticker.trim().uppercase(), category, selectedAccount!!.id)
                },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete investment?") },
            text = { Text("This will permanently delete \"${investment.name}\" and all its buy/sell events. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
