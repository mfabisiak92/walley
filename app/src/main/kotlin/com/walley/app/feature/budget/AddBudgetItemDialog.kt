package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.walley.app.domain.model.Currency

@Composable
fun AddBudgetItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: java.math.BigDecimal, paymentDay: Int?, isLastOfMonth: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var paymentDay by remember { mutableStateOf<Int?>(null) }
    var isLastOfMonth by remember { mutableStateOf(false) }

    val parsedAmount = amountText.toBigDecimalOrNull()
    val isValid = name.isNotBlank() && parsedAmount != null && parsedAmount.signum() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
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
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${Currency.PLN.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountText.isNotBlank() && parsedAmount == null
                )
                PaymentDaySelector(
                    paymentDay = paymentDay,
                    paymentDayIsLastOfMonth = isLastOfMonth,
                    onChange = { day, lastOfMonth ->
                        paymentDay = day
                        isLastOfMonth = lastOfMonth
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), parsedAmount!!, paymentDay, isLastOfMonth) },
                enabled = isValid
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
