package com.walley.app.feature.budget

import com.walley.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.walley.app.core.ui.BudgetItemIconPicker
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.EXPENSE_ICONS

/** Long-press edit dialog for an Ad-hoc item — icon only; the amount is edited from [MarkAdHocItemPaidDialog] instead. */
@Composable
fun EditAdHocItemAmountDialog(
    item: AdHocBudgetItem,
    onDismiss: () -> Unit,
    onSave: (icon: BudgetItemIcon?) -> Unit,
    onDelete: () -> Unit
) {
    var icon by remember { mutableStateOf(item.icon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.budget_icon_label), style = MaterialTheme.typography.labelLarge)
                BudgetItemIconPicker(options = EXPENSE_ICONS, selected = icon, onSelect = { icon = it })
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.budget_delete_item)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(icon) }) { Text(stringResource(R.string.budget_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.budget_cancel)) }
        }
    )
}
