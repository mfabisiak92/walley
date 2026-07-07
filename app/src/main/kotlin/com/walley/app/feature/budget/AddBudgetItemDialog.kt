package com.walley.app.feature.budget

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
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.IncomeCategory
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetItemDialog(
    currency: Currency,
    initial: WizardItemDraft? = null,
    accounts: List<Account> = emptyList(),
    requireAccount: Boolean = false,
    showCategoryPicker: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        amount: BigDecimal,
        paymentDay: Int?,
        isLastOfMonth: Boolean,
        accountId: Long?,
        incomeCategory: IncomeCategory?
    ) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var paymentDay by remember { mutableStateOf(initial?.paymentDay) }
    var isLastOfMonth by remember { mutableStateOf(initial?.paymentDayIsLastOfMonth ?: false) }
    var accountId by remember {
        mutableStateOf(initial?.accountId ?: accounts.find { it.isDefault }?.id ?: accounts.firstOrNull()?.id)
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(initial?.incomeCategory ?: IncomeCategory.SALARY) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val selectedAccount = accounts.find { it.id == accountId }
    val parsedAmount = amountText.toBigDecimalOrNull()
    val isValid = name.isNotBlank() && parsedAmount != null && parsedAmount.signum() > 0 &&
        (!requireAccount || selectedAccount != null)

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
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                if (requireAccount) {
                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                            isError = selectedAccount == null,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            accounts.forEach { account ->
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
                }
                if (showCategoryPicker) {
                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            IncomeCategory.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        category = option
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
                    label = { Text("Amount (${currency.symbol})") },
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
                onClick = {
                    onConfirm(
                        name.trim(),
                        parsedAmount!!,
                        paymentDay,
                        isLastOfMonth,
                        selectedAccount?.id,
                        if (showCategoryPicker) category else null
                    )
                },
                enabled = isValid
            ) { Text(if (initial != null) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
