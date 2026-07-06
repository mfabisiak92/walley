package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountLinkedItemDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amount: BigDecimal, paymentDay: Int?, isLastOfMonth: Boolean) -> Unit
) {
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var paymentDay by remember { mutableStateOf<Int?>(null) }
    var isLastOfMonth by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == accountId }
    val parsedAmount = amountText.toBigDecimalOrNull()
    val isValid = selectedAccount != null && parsedAmount != null && parsedAmount.signum() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = accountMenuExpanded,
                    onExpandedChange = { accountMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    accountId = account.id
                                    accountMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                selectedAccount?.let { account ->
                    val info = if (account.type == AccountType.SAVING) {
                        "Currently: ${formatMoney(account.balance, account.currency)} · Target: " +
                            (account.targetAmount?.let { formatMoney(it, account.currency) } ?: "not set")
                    } else {
                        "Currently: ${formatMoney(account.balance, account.currency)}"
                    }
                    Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount" + (selectedAccount?.let { " (${it.currency.symbol})" } ?: "")) },
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
                onClick = { onConfirm(selectedAccount!!.id, parsedAmount!!, paymentDay, isLastOfMonth) },
                enabled = isValid
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
