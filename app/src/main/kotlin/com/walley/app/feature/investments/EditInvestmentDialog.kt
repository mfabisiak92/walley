package com.walley.app.feature.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Investment
import java.math.BigDecimal

@Composable
fun EditInvestmentDialog(
    investment: Investment,
    investmentAccounts: List<Account>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        ticker: String,
        quantity: BigDecimal,
        price: BigDecimal,
        accountId: Long?
    ) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(investment.name) }
    var ticker by remember { mutableStateOf(investment.ticker) }
    var quantityText by remember { mutableStateOf(investment.quantity.toPlainString()) }
    var priceText by remember { mutableStateOf(investment.price.toPlainString()) }
    var accountId by remember { mutableStateOf(investment.accountId) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val parsedQuantity = quantityText.toBigDecimalOrNull()
    val parsedPrice = priceText.toBigDecimalOrNull()
    val isValid = name.isNotBlank() && ticker.isNotBlank() && parsedQuantity != null && parsedPrice != null

    // Currency is fixed after creation, so valid targets are same-currency investment accounts.
    val selectableAccounts = investmentAccounts.filter { it.currency == investment.currency }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit investment") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantityText.isNotBlank() && parsedQuantity == null
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price per unit (${investment.currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceText.isNotBlank() && parsedPrice == null
                )
                InvestmentAccountDropdown(
                    accounts = selectableAccounts,
                    selectedAccountId = accountId,
                    onAccountSelected = { accountId = it }
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
                onClick = { onSave(name.trim(), ticker.trim().uppercase(), parsedQuantity!!, parsedPrice!!, accountId) },
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
            text = { Text("This will permanently delete \"${investment.name}\". This cannot be undone.") },
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
