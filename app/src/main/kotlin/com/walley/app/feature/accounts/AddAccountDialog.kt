package com.walley.app.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.walley.app.core.ui.FieldHint
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.feature.budget.AccountEffectsToggleRow
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    allowedTypes: List<AccountType>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        currency: Currency,
        initialBalance: BigDecimal,
        taxRate: AccountTaxRate,
        targetAmount: BigDecimal?,
        commissionFlat: BigDecimal,
        commissionPercent: BigDecimal,
        isVirtual: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(allowedTypes.first()) }
    var currency by remember { mutableStateOf(Currency.PLN) }
    var balanceText by remember { mutableStateOf("0") }
    var taxRate by remember { mutableStateOf(AccountTaxRate.STANDARD_19) }
    var targetAmountText by remember { mutableStateOf("") }
    var commissionFlatText by remember { mutableStateOf("") }
    var commissionPercentText by remember { mutableStateOf("") }
    var isVirtual by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
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
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text(if (allowedTypes.size == 1) "Add ${allowedTypes.first().label.lowercase()} account" else "Add account") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.6f).dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                ExposedDropdownMenuBox(
                    expanded = currencyMenuExpanded,
                    onExpandedChange = { currencyMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currency.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = currencyMenuExpanded,
                        onDismissRequest = { currencyMenuExpanded = false }
                    ) {
                        Currency.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    currency = option
                                    currencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                AccountEffectsToggleRow(
                    label = "Virtual account",
                    checked = isVirtual,
                    onCheckedChange = { isVirtual = it },
                    hint = "Doesn't exist in the real world — its balance is really an earmarked slice of " +
                        "another account's money, so it's excluded from net worth and other totals."
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Uninvested cash",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FieldHint(
                                "This is cash not yet invested. The account's total balance also includes " +
                                    "the current value of any linked investments."
                            )
                        }
                        OutlinedTextField(
                            value = balanceText,
                            onValueChange = { balanceText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = parsedBalance == null
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Flat commission (optional)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FieldHint(
                                "Commission charged per buy/sell trade — whichever of the two is higher. " +
                                    "Used as the default when logging an event, but you can override it per trade."
                            )
                        }
                        OutlinedTextField(
                            value = commissionFlatText,
                            onValueChange = { commissionFlatText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = !commissionFlatValid
                        )
                    }
                    OutlinedTextField(
                        value = commissionPercentText,
                        onValueChange = { commissionPercentText = it },
                        label = { Text("Commission % of trade value (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !commissionPercentValid
                    )
                } else {
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Initial balance") },
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
                    onConfirm(
                        name.trim(),
                        type,
                        currency,
                        parsedBalance!!,
                        taxRate,
                        if (isSaving) parsedTargetAmount else null,
                        if (isInvestment) parsedCommissionFlat ?: BigDecimal.ZERO else BigDecimal.ZERO,
                        if (isInvestment) parsedCommissionPercent ?: BigDecimal.ZERO else BigDecimal.ZERO,
                        isVirtual
                    )
                },
                enabled = isValid
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
