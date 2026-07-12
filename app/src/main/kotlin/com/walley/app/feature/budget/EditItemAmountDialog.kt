package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import com.walley.app.core.ui.BudgetItemIconPicker
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.EXPENSE_ICONS
import com.walley.app.domain.model.INCOME_ICONS
import com.walley.app.domain.model.allowedAccountTypes
import com.walley.app.domain.model.isAccountWithdrawal
import com.walley.app.domain.model.requiresAccount
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemAmountDialog(
    item: BudgetItem,
    accounts: List<Account> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (amount: BigDecimal, icon: BudgetItemIcon?, accountId: Long?) -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf(item.amount.toPlainString()) }
    var icon by remember { mutableStateOf(item.icon) }
    var accountId by remember { mutableStateOf(item.accountId) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    val parsedAmount = amountText.toBigDecimalOrNull()

    val accountRequired = item.section.requiresAccount
    val accountOptions = item.section.allowedAccountTypes()
        ?.let { types -> accounts.filter { it.type in types && !it.isClosed } }
        ?: emptyList()
    val selectedAccount = accounts.find { it.id == accountId }
    // For Savings/Investments the account is the item's identity, so the amount tracks its currency.
    val amountCurrency = selectedAccount?.currency?.takeIf {
        item.section == BudgetSectionType.SAVINGS || item.section == BudgetSectionType.INVESTMENTS
    } ?: item.currency

    // How much more would still need to be withdrawn beyond what's already been paid out of the account.
    val additionalWithdrawal = parsedAmount?.let { it - item.paidAmount }
    val exceedsSavingsBalance = item.section.isAccountWithdrawal && selectedAccount?.type == AccountType.SAVING &&
        additionalWithdrawal != null && additionalWithdrawal > selectedAccount.balance
    val isValid = parsedAmount != null && parsedAmount.signum() > 0 && !exceedsSavingsBalance &&
        (!accountRequired || selectedAccount != null)
    val iconOptions = when (item.section) {
        BudgetSectionType.INCOME -> INCOME_ICONS
        BudgetSectionType.SAVINGS, BudgetSectionType.INVESTMENTS -> null
        else -> EXPENSE_ICONS
    }
    // Same as BudgetItemRow/WizardItemRow: a SAVINGS/INVESTMENTS item's title always tracks its
    // linked account's current name (including live, as the dropdown selection changes below).
    val isAccountLinked = item.section == BudgetSectionType.SAVINGS || item.section == BudgetSectionType.INVESTMENTS
    val displayName = if (isAccountLinked) selectedAccount?.name ?: item.name else item.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Planned amount (${amountCurrency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountText.isNotBlank() && (parsedAmount == null || exceedsSavingsBalance)
                )
                if (exceedsSavingsBalance && selectedAccount != null) {
                    Text(
                        "Only ${formatMoney(selectedAccount.balance, selectedAccount.currency)} left in ${selectedAccount.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (accountOptions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: if (accountRequired) "" else "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                            isError = accountRequired && selectedAccount == null,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            if (!accountRequired) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        accountId = null
                                        accountMenuExpanded = false
                                    }
                                )
                            }
                            accountOptions.forEach { account ->
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
                }
                if (iconOptions != null) {
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    BudgetItemIconPicker(options = iconOptions, selected = icon, onSelect = { icon = it })
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete item") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedAmount!!, icon, accountId) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
