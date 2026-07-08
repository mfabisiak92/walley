package com.walley.app.feature.investments

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import com.walley.app.domain.model.Investment
import java.math.BigDecimal

/** Lightweight dialog for the common case of just marking a position to market — everything else stays as-is. */
@Composable
fun UpdateCurrentPriceDialog(
    investment: Investment,
    onDismiss: () -> Unit,
    onSave: (currentPrice: BigDecimal) -> Unit
) {
    var currentPriceText by remember { mutableStateOf(investment.currentPrice.toPlainString()) }
    val parsedCurrentPrice = currentPriceText.toBigDecimalOrNull()
    val isValid = parsedCurrentPrice != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update current price") },
        text = {
            Column {
                Text(
                    "${investment.name} (${investment.ticker})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentPriceText,
                    onValueChange = { currentPriceText = it },
                    label = { Text("Current price (${investment.currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isValid
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedCurrentPrice!!) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
