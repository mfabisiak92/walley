package com.walley.app.feature.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Asset
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun EditAssetDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onSave: (currentValue: BigDecimal) -> Unit,
    onDelete: () -> Unit
) {
    var currentValueText by remember { mutableStateOf(asset.currentValue.toPlainString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val parsedCurrentValue = currentValueText.toBigDecimalOrNullLenient()
    val isValid = parsedCurrentValue != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(asset.name)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.assets_delete_asset_description),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(
                        R.string.assets_purchased_info,
                        asset.purchaseDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        formatMoney(asset.purchaseValue, asset.currency)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentValueText,
                    onValueChange = { currentValueText = it },
                    label = {
                        Text(
                            stringResource(
                                R.string.assets_label_current_value_with_currency,
                                asset.currency.symbol
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = parsedCurrentValue == null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedCurrentValue!!) },
                enabled = isValid
            ) { Text(stringResource(R.string.assets_action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.assets_action_cancel)) }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.assets_delete_asset_title)) },
            text = { Text(stringResource(R.string.assets_delete_confirm_message, asset.name)) },
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
