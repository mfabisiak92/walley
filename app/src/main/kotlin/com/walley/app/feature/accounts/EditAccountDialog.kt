package com.walley.app.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.feature.budget.AccountEffectsToggleRow
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAccountDialog(
    account: Account,
    allowedTypes: List<AccountType>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        newBalance: BigDecimal,
        targetAmount: BigDecimal?,
        commissionFlat: BigDecimal,
        commissionPercent: BigDecimal,
        isVirtual: Boolean
    ) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var type by remember { mutableStateOf(account.type) }
    var taxRate by remember { mutableStateOf(account.taxRate) }
    var isVirtual by remember { mutableStateOf(account.isVirtual) }
    var balanceText by remember {
        mutableStateOf(
            if (account.type == AccountType.INVESTMENT) {
                account.uninvestedCash.toPlainString()
            } else {
                account.balance.toPlainString()
            }
        )
    }
    var targetAmountText by remember { mutableStateOf(account.targetAmount?.toPlainString() ?: "") }
    var commissionFlatText by remember { mutableStateOf(account.commissionFlat.toPlainString()) }
    var commissionPercentText by remember { mutableStateOf(account.commissionPercent.toPlainString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var taxRateMenuExpanded by remember { mutableStateOf(false) }

    val parsedBalance = balanceText.toBigDecimalOrNull()
    val isInvestment = type == AccountType.INVESTMENT
    val isSaving = type == AccountType.SAVING
    val parsedTargetAmount = targetAmountText.toBigDecimalOrNull()
    val targetAmountValid = targetAmountText.isBlank() || parsedTargetAmount != null
    val parsedCommissionFlat = commissionFlatText.toBigDecimalOrNull()
    val commissionFlatValid = commissionFlatText.isBlank() || parsedCommissionFlat != null
    val parsedCommissionPercent = commissionPercentText.toBigDecimalOrNull()
    val commissionPercentValid = commissionPercentText.isBlank() || parsedCommissionPercent != null
    val isValid = name.isNotBlank() && parsedBalance != null && targetAmountValid &&
        commissionFlatValid && commissionPercentValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit account")
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete account",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                if (allowedTypes.size > 1) {
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = type.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            allowedTypes.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        type = option
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                AccountEffectsToggleRow(
                    label = "Virtual account",
                    checked = isVirtual,
                    onCheckedChange = { isVirtual = it }
                )
                Text(
                    "Doesn't exist in the real world — its balance is really an earmarked slice of " +
                        "another account's money, so it's excluded from net worth and other totals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isInvestment) {
                    ExposedDropdownMenuBox(
                        expanded = taxRateMenuExpanded,
                        onExpandedChange = { taxRateMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = taxRate.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tax rate") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taxRateMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = taxRateMenuExpanded,
                            onDismissRequest = { taxRateMenuExpanded = false }
                        ) {
                            AccountTaxRate.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        taxRate = option
                                        taxRateMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        "This is cash not yet invested. The account's total balance also includes " +
                            "the current value of any linked investments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Uninvested cash (${account.currency.symbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = parsedBalance == null
                    )
                    Text(
                        "Commission charged per buy/sell trade — whichever of the two is higher. " +
                            "Used as the default when logging an event, but you can override it per trade.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = commissionFlatText,
                        onValueChange = { commissionFlatText = it },
                        label = { Text("Flat commission (${account.currency.symbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !commissionFlatValid
                    )
                    OutlinedTextField(
                        value = commissionPercentText,
                        onValueChange = { commissionPercentText = it },
                        label = { Text("Commission % of trade value") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !commissionPercentValid
                    )
                } else {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Balance (${account.currency.symbol})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = parsedBalance == null
                    )
                }
                if (isSaving) {
                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = { targetAmountText = it },
                        label = { Text("Target amount (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !targetAmountValid
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name.trim(),
                        type,
                        taxRate,
                        parsedBalance!!,
                        if (isSaving) parsedTargetAmount else null,
                        if (isInvestment) parsedCommissionFlat ?: BigDecimal.ZERO else BigDecimal.ZERO,
                        if (isInvestment) parsedCommissionPercent ?: BigDecimal.ZERO else BigDecimal.ZERO,
                        isVirtual
                    )
                },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete account?") },
            text = { Text("This will permanently delete \"${account.name}\". This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
