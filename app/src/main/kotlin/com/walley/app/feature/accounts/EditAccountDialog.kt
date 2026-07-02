package com.walley.app.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.Account
import java.math.BigDecimal

@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onSave: (name: String, newBalance: BigDecimal) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var balanceText by remember { mutableStateOf(account.balance.toPlainString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val parsedBalance = balanceText.toBigDecimalOrNull()
    val isValid = name.isNotBlank() && parsedBalance != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Balance (${account.currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = parsedBalance == null
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete account")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), parsedBalance!!) },
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
            title = { Text("Delete account?") },
            text = { Text("This will permanently delete \"${account.name}\". This cannot be undone.") },
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
