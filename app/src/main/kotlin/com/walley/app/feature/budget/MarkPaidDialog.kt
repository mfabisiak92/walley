package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.BudgetItem
import java.math.BigDecimal

@Composable
fun MarkPaidDialog(
    item: BudgetItem,
    onDismiss: () -> Unit,
    onMarkFullyPaid: () -> Unit,
    onMarkPartiallyPaid: (BigDecimal) -> Unit
) {
    var amountText by remember { mutableStateOf(item.paidAmount.toPlainString()) }
    val parsedAmount = amountText.toBigDecimalOrNull()
    val isValid = parsedAmount != null && parsedAmount.signum() >= 0 && parsedAmount <= item.amount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Total: ${formatMoney(item.amount, item.currency)} · Paid so far: ${formatMoney(item.paidAmount, item.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Paid amount (${item.currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isValid
                )
                TextButton(onClick = onMarkFullyPaid) {
                    Text("Mark fully paid")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onMarkPartiallyPaid(parsedAmount!!) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
