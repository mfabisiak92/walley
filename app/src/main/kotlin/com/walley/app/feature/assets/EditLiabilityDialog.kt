package com.walley.app.feature.assets

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
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Liability
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun EditLiabilityDialog(
    liability: Liability,
    onDismiss: () -> Unit,
    onSave: (currentBalance: BigDecimal) -> Unit,
    onDelete: () -> Unit
) {
    var currentBalanceText by remember { mutableStateOf(liability.currentBalance.toPlainString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val parsedCurrentBalance = currentBalanceText.toBigDecimalOrNull()
    val isValid = parsedCurrentBalance != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(liability.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Started ${liability.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} at " +
                        formatMoney(liability.originalAmount, liability.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentBalanceText,
                    onValueChange = { currentBalanceText = it },
                    label = { Text("Current balance (${liability.currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = parsedCurrentBalance == null
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete liability")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedCurrentBalance!!) },
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
            title = { Text("Delete liability?") },
            text = { Text("This will permanently delete \"${liability.name}\". This cannot be undone.") },
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
