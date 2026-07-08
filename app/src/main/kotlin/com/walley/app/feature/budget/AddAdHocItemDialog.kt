package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.EXPENSE_ICONS
import com.walley.app.domain.model.suggestIconForName
import java.math.BigDecimal

/** Add/edit dialog for an Ad-hoc budget item — same fields as [AddBudgetItemDialog] minus account/payment day/category, since those don't apply to Ad-hoc items. */
@Composable
fun AddAdHocItemDialog(
    currency: Currency,
    initial: AdHocItemDraft? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: BigDecimal, icon: BudgetItemIcon?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toPlainString() ?: "") }
    var icon by remember { mutableStateOf(initial?.icon) }
    // Once the user manually picks an icon, stop overriding it as they keep editing the name.
    var iconAutoAssigned by remember { mutableStateOf(initial?.icon == null) }

    val parsedAmount = amountText.toBigDecimalOrNull()
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
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountText.isNotBlank() && parsedAmount == null
                )
                Text("Icon", style = MaterialTheme.typography.labelLarge)
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
                onClick = { onConfirm(name.trim(), parsedAmount!!, icon) },
                enabled = isValid
            ) { Text(if (initial != null) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
