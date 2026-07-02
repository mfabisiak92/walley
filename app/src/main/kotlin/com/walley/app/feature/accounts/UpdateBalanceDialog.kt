package com.walley.app.feature.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.Account
import java.math.BigDecimal

@Composable
fun UpdateBalanceDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (newBalance: BigDecimal) -> Unit
) {
    var balanceText by remember { mutableStateOf(account.balance.toPlainString()) }
    val parsedBalance = balanceText.toBigDecimalOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update balance") },
        text = {
            Column {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Balance (${account.currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = parsedBalance == null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(parsedBalance!!) },
                enabled = parsedBalance != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
