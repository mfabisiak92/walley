package com.walley.app.feature.budget

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
import com.walley.app.core.ui.BudgetItemIconPicker
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.EXPENSE_ICONS
import java.math.BigDecimal

/**
 * Long-press edit dialog for an Ad-hoc item. [accountBalance]/[currency] are the item's own
 * effective account's (its override, or the budget's default) — there's no account picker here,
 * since which account an item draws from is set once, in the wizard.
 */
@Composable
fun EditAdHocItemAmountDialog(
    item: AdHocBudgetItem,
    accountBalance: BigDecimal,
    currency: Currency,
    onDismiss: () -> Unit,
    onSave: (amount: BigDecimal, icon: BudgetItemIcon?) -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf(item.amount.toPlainString()) }
    var icon by remember { mutableStateOf(item.icon) }
    val parsedAmount = amountText.toBigDecimalOrNull()

    // How much more would still need to be withdrawn beyond what's already been paid out of the account.
    val additionalWithdrawal = parsedAmount?.let { it - item.paidAmount }
    val exceedsBalance = additionalWithdrawal != null && additionalWithdrawal > accountBalance
    val isValid = parsedAmount != null && parsedAmount.signum() > 0 && !exceedsBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Planned amount (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountText.isNotBlank() && (parsedAmount == null || exceedsBalance)
                )
                if (exceedsBalance) {
                    Text(
                        "Only ${formatMoney(accountBalance, currency)} left in the linked account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                BudgetItemIconPicker(options = EXPENSE_ICONS, selected = icon, onSelect = { icon = it })
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete item") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedAmount!!, icon) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
