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
import com.walley.app.domain.model.Asset
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun EditAssetDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onSave: (currentValue: BigDecimal) -> Unit,
    onDelete: () -> Unit
) {
    var currentValueText by remember { mutableStateOf(asset.currentValue.toPlainString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val parsedCurrentValue = currentValueText.toBigDecimalOrNull()
    val isValid = parsedCurrentValue != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(asset.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Purchased ${asset.purchaseDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} for " +
                        formatMoney(asset.purchaseValue, asset.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentValueText,
                    onValueChange = { currentValueText = it },
                    label = { Text("Current value (${asset.currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = parsedCurrentValue == null
                )
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete asset")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedCurrentValue!!) },
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
            title = { Text("Delete asset?") },
            text = { Text("This will permanently delete \"${asset.name}\". This cannot be undone.") },
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
