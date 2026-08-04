package com.walley.app.feature.budget

import com.walley.app.core.format.toBigDecimalOrNullLenient
import com.walley.app.R
import androidx.compose.ui.res.stringResource
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
import com.walley.app.core.ui.BudgetItemIconPicker
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.EXPENSE_ICONS
import com.walley.app.domain.model.suggestIconForName
import java.math.BigDecimal

/**
 * Add/edit dialog for an Ad-hoc budget item — same fields as [AddBudgetItemDialog] minus payment
 * day/category, since those don't apply to Ad-hoc items. The account picker only appears when
 * [accounts] has more than one option; a single-account budget stays exactly as before.
 */
@Composable
fun AddAdHocItemDialog(
    defaultAccount: Account,
    accounts: List<Account>,
    initial: AdHocItemDraft? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: BigDecimal, icon: BudgetItemIcon?, accountId: Long?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var icon by remember { mutableStateOf(initial?.icon) }
    var selectedAccountId by remember { mutableStateOf(initial?.accountId) }
    // Once the user manually picks an icon, stop overriding it as they keep editing the name.
    var iconAutoAssigned by remember { mutableStateOf(initial?.icon == null) }

    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: defaultAccount
    val parsedAmount = amountText.toBigDecimalOrNullLenient()
    val isValid = name.isNotBlank() && parsedAmount != null && parsedAmount.signum() > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Edit item" else "Add item") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (iconAutoAssigned) {
                            suggestIconForName(it)?.let { suggested -> icon = suggested }
                        }
                    },
                    label = { Text(stringResource(R.string.budget_name_label)) },
                    singleLine = true
                )
                if (accounts.size > 1) {
                    AccountPicker(
                        accounts = accounts,
                        defaultAccount = defaultAccount,
                        selectedAccountId = selectedAccountId,
                        onSelect = { selectedAccountId = it }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.budget_amount_with_currency, selectedAccount.currency.symbol)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountText.isNotBlank() && parsedAmount == null
                )
                Text(stringResource(R.string.budget_icon_label), style = MaterialTheme.typography.labelLarge)
                BudgetItemIconPicker(
                    options = EXPENSE_ICONS,
                    selected = icon,
                    onSelect = {
                        icon = it
                        iconAutoAssigned = false
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), parsedAmount!!, icon, selectedAccountId) },
                enabled = isValid
            ) { Text(if (initial != null) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.budget_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPicker(
    accounts: List<Account>,
    defaultAccount: Account,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: defaultAccount

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedAccount.name + if (selectedAccount.id == defaultAccount.id) " (default)" else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.budget_draw_from_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name + if (account.id == defaultAccount.id) " (default)" else "") },
                    onClick = {
                        onSelect(if (account.id == defaultAccount.id) null else account.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
