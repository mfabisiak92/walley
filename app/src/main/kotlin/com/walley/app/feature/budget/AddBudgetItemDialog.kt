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
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.BudgetItemIconPicker
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.EXPENSE_ICONS
import com.walley.app.domain.model.INCOME_ICONS
import com.walley.app.domain.model.IncomeCategory
import com.walley.app.domain.model.displayName
import com.walley.app.domain.model.suggestIconForName
import com.walley.app.domain.model.toIcon
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetItemDialog(
    currency: Currency,
    initial: WizardItemDraft? = null,
    accounts: List<Account> = emptyList(),
    requireAccount: Boolean = false,
    showAccountPicker: Boolean = requireAccount,
    showCategoryPicker: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        amount: BigDecimal,
        paymentDay: Int?,
        isLastOfMonth: Boolean,
        accountId: Long?,
        incomeCategory: IncomeCategory?,
        icon: BudgetItemIcon?
    ) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var paymentDay by remember { mutableStateOf(initial?.paymentDay) }
    var isLastOfMonth by remember { mutableStateOf(initial?.paymentDayIsLastOfMonth ?: false) }
    val selectableAccounts = accounts.filterNot { it.isClosed }
    var accountId by remember {
        mutableStateOf(
            initial?.accountId
                ?: (if (requireAccount) {
                    selectableAccounts.find { it.isDefault }?.id ?: selectableAccounts.firstOrNull()?.id
                } else {
                    null
                })
        )
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(initial?.incomeCategory ?: IncomeCategory.SALARY) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var icon by remember { mutableStateOf(initial?.icon ?: category.takeIf { showCategoryPicker }?.toIcon()) }
    // Once the user manually picks an icon, stop overriding it as they keep editing the name.
    var iconAutoAssigned by remember { mutableStateOf(!showCategoryPicker && initial?.icon == null) }
    val iconOptions = if (showCategoryPicker) INCOME_ICONS else EXPENSE_ICONS

    val selectedAccount = accounts.find { it.id == accountId }
    val parsedAmount = amountText.toBigDecimalOrNullLenient()
    val exceedsSavingsBalance = selectedAccount?.type == AccountType.SAVING &&
        parsedAmount != null && parsedAmount > selectedAccount.balance
    val isValid = name.isNotBlank() && parsedAmount != null && parsedAmount.signum() > 0 &&
        (!requireAccount || selectedAccount != null) && !exceedsSavingsBalance

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
                if (showAccountPicker && accounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: if (requireAccount) "" else "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.budget_account_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                            isError = requireAccount && selectedAccount == null,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            if (!requireAccount) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.budget_none)) },
                                    onClick = {
                                        accountId = null
                                        accountMenuExpanded = false
                                    }
                                )
                            }
                            selectableAccounts.forEach { account ->
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
                        Text(
                            "Currently: ${formatMoney(account.balance, account.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (exceedsSavingsBalance) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (exceedsSavingsBalance) {
                            Text(
                                "Amount exceeds this account's balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (showCategoryPicker) {
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.budget_category_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            IncomeCategory.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName()) },
                                    onClick = {
                                        category = option
                                        icon = option.toIcon()
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(stringResource(R.string.budget_amount_with_currency, (selectedAccount?.currency ?: currency).symbol)) },
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
                Text(stringResource(R.string.budget_icon_label), style = MaterialTheme.typography.labelLarge)
                BudgetItemIconPicker(
                    options = iconOptions,
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
                onClick = {
                    onConfirm(
                        name.trim(),
                        parsedAmount!!,
                        paymentDay,
                        isLastOfMonth,
                        selectedAccount?.id,
                        if (showCategoryPicker) category else null,
                        icon
                    )
                },
                enabled = isValid
            ) { Text(if (initial != null) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.budget_cancel)) }
        }
    )
}
