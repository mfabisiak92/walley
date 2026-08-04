package com.walley.app.feature.budget

import com.walley.app.core.format.toBigDecimalOrNullLenient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import com.walley.app.core.ui.InvestmentGainColor
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountLinkedItemDialog(
    accounts: List<Account>,
    initial: WizardItemDraft? = null,
    // Accounts already linked to another item of the same section elsewhere in this budget/wizard —
    // callers must exclude the item being edited's own account so it stays selectable.
    excludedAccountIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, amount: BigDecimal, paymentDay: Int?, isLastOfMonth: Boolean) -> Unit
) {
    val selectableAccounts = accounts.filterNot { it.isClosed || it.id in excludedAccountIds }
    var accountId by remember { mutableStateOf(initial?.accountId ?: selectableAccounts.firstOrNull()?.id) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var paymentDay by remember { mutableStateOf(initial?.paymentDay) }
    var isLastOfMonth by remember { mutableStateOf(initial?.paymentDayIsLastOfMonth ?: false) }

    val selectedAccount = accounts.find { it.id == accountId }
    val parsedAmount = amountText.toBigDecimalOrNullLenient()
    val isValid = selectedAccount != null && parsedAmount != null && parsedAmount.signum() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial != null) R.string.budget_edit_item else R.string.budget_add_item)) },
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
                        label = { Text(stringResource(R.string.budget_account_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false }
                    ) {
                        selectableAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                trailingIcon = {
                                    if (account.targetReached) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = stringResource(R.string.budget_target_reached_cd),
                                            tint = InvestmentGainColor
                                        )
                                    }
                                },
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
                        stringResource(
                            R.string.budget_currently_amount_target,
                            formatMoney(account.balance, account.currency),
                            account.targetAmount?.let { formatMoney(it, account.currency) }
                                ?: stringResource(R.string.budget_target_not_set)
                        )
                    } else {
                        stringResource(R.string.budget_currently_amount, formatMoney(account.balance, account.currency))
                    }
                    Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = {
                        Text(
                            selectedAccount?.let { stringResource(R.string.budget_amount_with_currency, it.currency.symbol) }
                                ?: stringResource(R.string.budget_amount_label)
                        )
                    },
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
            ) { Text(stringResource(if (initial != null) R.string.budget_save else R.string.budget_add_button)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.budget_cancel)) }
        }
    )
}
