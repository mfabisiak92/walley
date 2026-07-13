package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.Currency
import java.math.BigDecimal

/**
 * Tap-to-pay dialog for an Ad-hoc item — mirrors [MarkPaidDialog]: paid and planned amounts edited
 * side by side, plus a title-bar checkmark shortcut to mark it paid in full instantly.
 */
@Composable
fun MarkAdHocItemPaidDialog(
    item: AdHocBudgetItem,
    currency: Currency,
    accountBalance: BigDecimal,
    onDismiss: () -> Unit,
    onMarkFullyPaid: () -> Unit,
    onSave: (paidAmount: BigDecimal, plannedAmount: BigDecimal) -> Unit
) {
    var paidText by remember { mutableStateOf(item.paidAmount.toPlainString()) }
    var plannedText by remember { mutableStateOf(item.amount.toPlainString()) }
    val parsedPaid = paidText.toBigDecimalOrNull()
    val parsedPlanned = plannedText.toBigDecimalOrNull()

    // How much more of the (possibly edited) planned amount would still need to come out of the
    // linked account beyond what's already paid.
    val additionalWithdrawal = parsedPlanned?.let { it - item.paidAmount }
    val exceedsBalance = additionalWithdrawal != null && additionalWithdrawal > accountBalance
    val isValid = parsedPaid != null && parsedPlanned != null &&
        parsedPaid.signum() >= 0 && parsedPlanned.signum() > 0 &&
        parsedPaid <= parsedPlanned && !exceedsBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!item.isCompleted) {
                    IconButton(onClick = onMarkFullyPaid) {
                        Icon(Icons.Filled.Check, contentDescription = "Mark as fully paid")
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paidText,
                        onValueChange = { paidText = it },
                        label = { Text("Paid") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = parsedPaid == null || (parsedPlanned != null && parsedPaid > parsedPlanned),
                        modifier = Modifier.weight(1f)
                    )
                    Text("/", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = plannedText,
                        onValueChange = { plannedText = it },
                        label = { Text("Planned (${currency.symbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = parsedPlanned == null || exceedsBalance,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (exceedsBalance) {
                    Text(
                        "Only ${formatMoney(accountBalance, currency)} left in the linked account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedPaid!!, parsedPlanned!!) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
