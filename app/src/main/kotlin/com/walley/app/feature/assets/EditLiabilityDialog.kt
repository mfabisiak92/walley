package com.walley.app.feature.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.walley.app.core.format.toBigDecimalOrNullLenient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Liability
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun EditLiabilityDialog(
    liability: Liability,
    onDismiss: () -> Unit,
    onSave: (currentBalance: BigDecimal) -> Unit,
    onDelete: () -> Unit
) {
    var currentBalanceText by remember { mutableStateOf(liability.currentBalance.toPlainString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val parsedCurrentBalance = currentBalanceText.toBigDecimalOrNullLenient()
    val isValid = parsedCurrentBalance != null
    // Once fully paid there's nothing left to owe, so the balance is locked rather than editable again.
    val isFullyPaid = liability.currentBalance.signum() == 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    liability.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!isFullyPaid) {
                    IconButton(onClick = { onSave(BigDecimal.ZERO) }) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.assets_mark_fully_paid_description)
                        )
                    }
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.assets_delete_liability_description),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.assets_started_info,
                        liability.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        formatMoney(liability.originalAmount, liability.currency)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentBalanceText,
                    onValueChange = { currentBalanceText = it },
                    label = {
                        Text(
                            stringResource(
                                R.string.assets_label_current_balance_with_currency,
                                liability.currency.symbol
                            )
                        )
                    },
                    singleLine = true,
                    enabled = !isFullyPaid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = parsedCurrentBalance == null
                )
                if (isFullyPaid) {
                    Text(
                        stringResource(R.string.assets_liability_fully_paid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedCurrentBalance!!) },
                enabled = isValid && !isFullyPaid
            ) { Text(stringResource(R.string.assets_action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.assets_action_cancel)) }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.assets_delete_liability_title)) },
            text = { Text(stringResource(R.string.assets_delete_confirm_message, liability.name)) },
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.assets_action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.assets_action_cancel)) }
            }
        )
    }
}
