package com.walley.app.feature.budget

import com.walley.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.BudgetItemIconPicker
import com.walley.app.core.ui.InvestmentGainColor
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

/** Long-press edit dialog — account and icon only; the amount is edited from [MarkPaidDialog] instead. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemAmountDialog(
    item: BudgetItem,
    accounts: List<Account> = emptyList(),
    // Accounts already linked to another item of the same section elsewhere in this budget — callers
    // must exclude [item]'s own account so it stays selectable.
    excludedAccountIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (icon: BudgetItemIcon?, accountId: Long?) -> Unit,
    onDelete: () -> Unit
) {
    var icon by remember { mutableStateOf(item.icon) }
    var accountId by remember { mutableStateOf(item.accountId) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val accountRequired = item.section.requiresAccount
    val accountOptions = item.section.allowedAccountTypes()
        ?.let { types -> accounts.filter { it.type in types && !it.isClosed && it.id !in excludedAccountIds } }
        ?: emptyList()
    val selectedAccount = accounts.find { it.id == accountId }

    // How much more of the (fixed) planned amount would still need to be withdrawn beyond what's paid.
    val additionalWithdrawal = item.amount - item.paidAmount
    val exceedsSavingsBalance = item.section.isAccountWithdrawal && selectedAccount?.type == AccountType.SAVING &&
        additionalWithdrawal > selectedAccount.balance
    val isValid = !exceedsSavingsBalance && (!accountRequired || selectedAccount != null)
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
                if (accountOptions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: if (accountRequired) "" else "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.budget_account_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                            isError = (accountRequired && selectedAccount == null) || exceedsSavingsBalance,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            if (!accountRequired) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.budget_none)) },
                                    onClick = {
                                        accountId = null
                                        accountMenuExpanded = false
                                    }
                                )
                            }
                            accountOptions.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.name) },
                                    trailingIcon = {
                                        if (account.targetReached) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = "Target reached",
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
                            "Currently: ${formatMoney(account.balance, account.currency)} · Target: " +
                                (account.targetAmount?.let { formatMoney(it, account.currency) } ?: "not set")
                        } else {
                            "Currently: ${formatMoney(account.balance, account.currency)}"
                        }
                        Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (exceedsSavingsBalance && selectedAccount != null) {
                        Text(
                            "Only ${formatMoney(selectedAccount.balance, selectedAccount.currency)} left in ${selectedAccount.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (iconOptions != null) {
                    Text(stringResource(R.string.budget_icon_label), style = MaterialTheme.typography.labelLarge)
                    BudgetItemIconPicker(options = iconOptions, selected = icon, onSelect = { icon = it })
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.budget_delete_item)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(icon, accountId) },
                enabled = isValid
            ) { Text(stringResource(R.string.budget_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.budget_cancel)) }
        }
    )
}
