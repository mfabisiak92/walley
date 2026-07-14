package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
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
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.isAccountWithdrawal
import com.walley.app.domain.model.isFinalizable
import java.math.BigDecimal

/**
 * Lets the paid and planned amounts be edited side by side, as "paid / planned" — mirroring how
 * [BudgetItemRow] already displays them. The title-bar checkmark (hidden once already fully paid,
 * same as [com.walley.app.feature.assets.EditLiabilityDialog]'s) is a one-tap shortcut that ignores
 * whatever's currently typed and marks the item paid in full immediately. For Income/Income-related-
 * expenses items, a lock icon next to it lets the item be finalized (locking its amounts for good)
 * even while only partially paid — always behind a confirmation, since it can't be undone.
 */
@Composable
fun MarkPaidDialog(
    item: BudgetItem,
    accounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onMarkFullyPaid: () -> Unit,
    onComplete: () -> Unit,
    onSave: (paidAmount: BigDecimal, plannedAmount: BigDecimal) -> Unit
) {
    var paidText by remember { mutableStateOf(item.paidAmount.toPlainString()) }
    var plannedText by remember { mutableStateOf(item.amount.toPlainString()) }
    var showCompleteConfirm by remember { mutableStateOf(false) }
    val parsedPaid = paidText.toBigDecimalOrNull()
    val parsedPlanned = plannedText.toBigDecimalOrNull()

    // Only relevant when this item's payment withdraws from a Saving account — how much more of the
    // (possibly edited) planned amount would still need to come out of it beyond what's already paid.
    val linkedAccount = accounts.find { it.id == item.accountId }
    val additionalWithdrawal = parsedPlanned?.let { it - item.paidAmount }
    val exceedsSavingsBalance = item.section.isAccountWithdrawal && linkedAccount?.type == AccountType.SAVING &&
        additionalWithdrawal != null && additionalWithdrawal > linkedAccount.balance
    val isValid = !item.isFinalized && parsedPaid != null && parsedPlanned != null &&
        parsedPaid.signum() >= 0 && parsedPlanned.signum() > 0 &&
        parsedPaid <= parsedPlanned && !exceedsSavingsBalance

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
                if (!item.isCompleted && !item.isFinalized) {
                    IconButton(onClick = onMarkFullyPaid) {
                        Icon(Icons.Filled.Check, contentDescription = "Mark as fully paid")
                    }
                }
                if (item.section.isFinalizable && !item.isFinalized) {
                    IconButton(onClick = { showCompleteConfirm = true }) {
                        Icon(Icons.Filled.Lock, contentDescription = "Complete item")
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
                        label = { Text("Planned (${item.currency.symbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = parsedPlanned == null || exceedsSavingsBalance,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (exceedsSavingsBalance && linkedAccount != null) {
                    Text(
                        "Only ${formatMoney(linkedAccount.balance, linkedAccount.currency)} left in ${linkedAccount.name}",
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

    if (showCompleteConfirm) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirm = false },
            title = { Text("Complete this item?") },
            text = {
                Text(
                    "This is a one-way change. Once completed, \"${item.name}\" can no longer be edited " +
                        "or paid further, even if it isn't fully paid — whatever's paid now becomes final."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompleteConfirm = false
                        onComplete()
                    }
                ) { Text("Complete") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
