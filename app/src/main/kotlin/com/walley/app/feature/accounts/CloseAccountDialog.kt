package com.walley.app.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseAccountDialog(
    account: Account,
    otherAccounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (transferToAccountId: Long?) -> Unit
) {
    // For an Investment account, the stored balance column is uninvested cash — account.balance also
    // includes the market value of any linked investments, which never moves as part of closing.
    val transferAmount = if (account.type == AccountType.INVESTMENT) account.uninvestedCash else account.balance
    val needsTransfer = !account.isVirtual && transferAmount.signum() != 0

    if (!needsTransfer) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Close account?") },
            text = { Text("Close \"${account.name}\"? You can reopen it later from the Accounts screen.") },
            confirmButton = {
                TextButton(onClick = { onConfirm(null) }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
        return
    }

    val candidates = otherAccounts.filter {
        it.id != account.id &&
            !it.isClosed &&
            it.currency == account.currency &&
            it.type in setOf(AccountType.CHECKING, AccountType.SAVING, AccountType.CASH)
    }
    var destinationId by remember { mutableStateOf(candidates.firstOrNull()?.id) }
    var menuExpanded by remember { mutableStateOf(false) }
    val destination = candidates.find { it.id == destinationId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Close account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The remaining ${formatMoney(transferAmount, account.currency)} in \"${account.name}\" " +
                        "will be transferred to the account you pick below. You can reopen \"${account.name}\" " +
                        "later from the Accounts screen, but the transfer won't be reversed automatically."
                )
                if (candidates.isEmpty()) {
                    Text(
                        "No other account in ${account.currency.name} is available to receive the balance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = destination?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Transfer to") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            candidates.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(candidate.name) },
                                    onClick = {
                                        destinationId = candidate.id
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(destination!!.id) },
                enabled = destination != null
            ) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
