package com.walley.app.feature.investments

import com.walley.app.core.format.toBigDecimalOrNullLenient
import com.walley.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Investment
import java.math.BigDecimal

/** Lightweight dialog for the common case of just marking a position to market — everything else stays as-is. */
@Composable
fun UpdateCurrentPriceDialog(
    investment: Investment,
    onDismiss: () -> Unit,
    onSave: (currentPrice: BigDecimal) -> Unit,
    onRevertToPrevious: () -> Unit
) {
    var currentPriceText by remember { mutableStateOf(investment.currentPrice.toPlainString()) }
    val parsedCurrentPrice = currentPriceText.toBigDecimalOrNullLenient()
    val isValid = parsedCurrentPrice != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.investments_update_current_price_title)) },
        text = {
            Column {
                Text(
                    "${investment.name} (${investment.ticker})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentPriceText,
                    onValueChange = { currentPriceText = it },
                    label = { Text(stringResource(R.string.investments_label_current_price_currency, investment.currency.symbol)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = !isValid
                )
                // Only a single step back is kept (the price in effect just before the most recent
                // update), so this is offered right where the user is already changing the price.
                investment.previousPrice?.let { previousPrice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                R.string.investments_previous_price_label,
                                formatMoney(previousPrice, investment.currency)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            onRevertToPrevious()
                            onDismiss()
                        }) { Text(stringResource(R.string.investments_action_revert)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(parsedCurrentPrice!!) },
                enabled = isValid
            ) { Text(stringResource(R.string.investments_action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.investments_action_cancel)) }
        }
    )
}
