package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.core.ui.BudgetItemIconPicker
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.EXPENSE_ICONS
import com.walley.app.domain.model.INCOME_ICONS
import java.math.BigDecimal

@Composable
fun EditItemAmountDialog(
    item: BudgetItem,
    onDismiss: () -> Unit,
    onSave: (amount: BigDecimal, icon: BudgetItemIcon?) -> Unit,
    onDelete: () -> Unit
) {
    var amountText by remember { mutableStateOf(item.amount.toPlainString()) }
    var icon by remember { mutableStateOf(item.icon) }
    val parsedAmount = amountText.toBigDecimalOrNull()
    val isValid = parsedAmount != null && parsedAmount.signum() > 0
    val iconOptions = when (item.section) {
        BudgetSectionType.INCOME -> INCOME_ICONS
        BudgetSectionType.SAVINGS, BudgetSectionType.INVESTMENTS -> null
        else -> EXPENSE_ICONS
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Planned amount (${item.currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountText.isNotBlank() && !isValid
                )
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
                onClick = { onSave(parsedAmount!!, icon) },
                enabled = isValid
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
