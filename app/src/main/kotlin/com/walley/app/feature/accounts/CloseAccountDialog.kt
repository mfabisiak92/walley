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
    val needsTransfer = transferAmount.signum() != 0

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

    // Only accounts in the same currency as the one being closed can receive the transfer. A virtual
    // account's balance is just an earmarked slice of a real account's money, so it may only move into
    // another virtual account; a non-virtual account's real balance can move into any other open account.
    val candidates = otherAccounts.filter {
        it.id != account.id &&
            !it.isClosed &&
            it.currency == account.currency &&
            (!account.isVirtual || it.isVirtual)
    }
    // The transfer is optional — default to not transferring so money never moves without the user
    // explicitly picking a destination.
    var destinationId by remember { mutableStateOf<Long?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val destination = candidates.find { it.id == destinationId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Close account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "\"${account.name}\" has a remaining balance of ${formatMoney(transferAmount, account.currency)}. " +
                        "You can optionally transfer it to another account below, or leave it in \"${account.name}\" " +
                        "— it'll stay there but be excluded from net worth while closed. You can reopen " +
                        "\"${account.name}\" later from the Accounts screen, but a transfer won't be reversed automatically."
                )
                if (candidates.isEmpty()) {
                    val virtualQualifier = if (account.isVirtual) "virtual " else ""
                    Text(
                        "No other ${virtualQualifier}account in ${account.currency.name} is available to receive " +
                            "the balance — it will stay in \"${account.name}\" if you close it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = destination?.name ?: "Don't transfer",
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
                            DropdownMenuItem(
                                text = { Text("Don't transfer") },
                                onClick = {
                                    destinationId = null
                                    menuExpanded = false
                                }
                            )
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
            TextButton(onClick = { onConfirm(destination?.id) }) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
