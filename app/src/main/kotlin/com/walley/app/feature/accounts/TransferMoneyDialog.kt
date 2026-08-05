package com.walley.app.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.core.format.toBigDecimalOrNullLenient
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import java.math.BigDecimal

/** What a transfer actually moves out of [account] — an Investment account's uninvested cash, never the market value of its linked investments. */
private fun transferableAmount(account: Account): BigDecimal =
    if (account.type == AccountType.INVESTMENT) account.uninvestedCash else account.balance

/**
 * Lets the user move money between any two of their own open accounts that share a currency — reachable
 * from any of the three Accounts tabs since a transfer isn't tied to one tab's account types (e.g.
 * Checking → Saving). Picking "From" narrows "To" down to same-currency accounts that are valid
 * destinations for it: any open account, unless "From" is virtual, in which case only another virtual
 * account qualifies (its balance is just an earmarked slice of a real account's money — see
 * [AccountRepository.closeAccount]'s matching rule). The amount is capped at [transferableAmount] so a
 * transfer can't put an account further into overdraft than it can actually cover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferMoneyDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (fromAccountId: Long, toAccountId: Long, amount: BigDecimal) -> Unit
) {
    val openAccounts = accounts.filterNot { it.isClosed }

    var fromId by remember { mutableStateOf<Long?>(null) }
    var toId by remember { mutableStateOf<Long?>(null) }
    var amountText by remember { mutableStateOf("") }
    var fromMenuExpanded by remember { mutableStateOf(false) }
    var toMenuExpanded by remember { mutableStateOf(false) }

    val fromAccount = openAccounts.find { it.id == fromId }
    val toCandidates = fromAccount?.let { from ->
        openAccounts.filter { it.id != from.id && it.currency == from.currency && (!from.isVirtual || it.isVirtual) }
    } ?: emptyList()
    val toAccount = toCandidates.find { it.id == toId }

    val maxAvailable = fromAccount?.let { transferableAmount(it) }
    val parsedAmount = amountText.toBigDecimalOrNullLenient()
    val amountExceedsBalance = parsedAmount != null && maxAvailable != null && parsedAmount > maxAvailable
    val amountValid = parsedAmount != null && parsedAmount.signum() > 0 && !amountExceedsBalance
    val canConfirm = fromAccount != null && toAccount != null && amountValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accounts_transfer_money_title)) },
        text = {
            if (openAccounts.size < 2) {
                Text(
                    stringResource(R.string.accounts_transfer_needs_two_accounts),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(expanded = fromMenuExpanded, onExpandedChange = { fromMenuExpanded = it }) {
                        OutlinedTextField(
                            value = fromAccount?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.accounts_transfer_from)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = fromMenuExpanded, onDismissRequest = { fromMenuExpanded = false }) {
                            openAccounts.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(candidate.name) },
                                    onClick = {
                                        if (candidate.id != fromId) {
                                            // A currency (or virtual-ness) change can invalidate whatever "To" was
                                            // picked before — safer to always clear it than leave a stale choice
                                            // that no longer appears in the (now different) candidate list.
                                            toId = null
                                        }
                                        fromId = candidate.id
                                        fromMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (fromAccount != null) {
                        Text(
                            stringResource(R.string.accounts_transfer_available, formatMoney(maxAvailable ?: BigDecimal.ZERO, fromAccount.currency)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (toCandidates.isEmpty()) {
                            Text(
                                stringResource(R.string.accounts_transfer_no_destination, fromAccount.currency.name),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            ExposedDropdownMenuBox(expanded = toMenuExpanded, onExpandedChange = { toMenuExpanded = it }) {
                                OutlinedTextField(
                                    value = toAccount?.name.orEmpty(),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.accounts_transfer_to)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toMenuExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(expanded = toMenuExpanded, onDismissRequest = { toMenuExpanded = false }) {
                                    toCandidates.forEach { candidate ->
                                        DropdownMenuItem(
                                            text = { Text(candidate.name) },
                                            onClick = {
                                                toId = candidate.id
                                                toMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                label = { Text(stringResource(R.string.accounts_transfer_amount_with_currency, fromAccount.currency.symbol)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = amountText.isNotBlank() && !amountValid
                            )
                            if (amountExceedsBalance) {
                                Text(
                                    stringResource(R.string.accounts_transfer_exceeds_balance),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(fromAccount!!.id, toAccount!!.id, parsedAmount!!) },
                enabled = canConfirm
            ) { Text(stringResource(R.string.accounts_transfer_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accounts_cancel)) }
        }
    )
}
