package com.walley.app.feature.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.InvestmentTransaction
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.availableCashToBuy
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * When [initial] is null this is a "quick add" launched from a dedicated Buy or Sell button, so the
 * type is fixed to [defaultType] and not shown as a choice. When editing an existing event, the type
 * stays changeable in case it was logged as the wrong one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentTransactionDialog(
    data: InvestmentWithTransactions,
    account: Account?,
    investmentsInAccount: List<InvestmentWithTransactions>,
    defaultType: InvestmentTransactionType = InvestmentTransactionType.BUY,
    initial: InvestmentTransaction? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal
    ) -> Unit
) {
    val currency = data.investment.currency
    var type by remember { mutableStateOf(initial?.type ?: defaultType) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(initial?.date ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf(initial?.quantity?.toPlainString() ?: "") }
    var priceText by remember { mutableStateOf(initial?.pricePerUnit?.toPlainString() ?: "") }
    var commissionText by remember { mutableStateOf(initial?.commission?.toPlainString() ?: "") }

    val parsedQuantity = quantityText.toBigDecimalOrNull()
    val parsedPrice = priceText.toBigDecimalOrNull()
    val isEditing = initial != null

    val tradeValue = if (parsedQuantity != null && parsedPrice != null) parsedQuantity * parsedPrice else BigDecimal.ZERO
    val suggestedCommission = account?.defaultCommission(tradeValue)
    val parsedCommission = commissionText.toBigDecimalOrNull()
    val effectiveCommission = parsedCommission ?: suggestedCommission ?: BigDecimal.ZERO
    val commissionInvalid = commissionText.isNotBlank() && (parsedCommission == null || parsedCommission.signum() < 0)

    val availableToSell = if (type == InvestmentTransactionType.SELL) {
        data.quantityAvailableOn(date, excludingTransactionId = initial?.id)
    } else {
        null
    }
    val exceedsHoldings = availableToSell != null && parsedQuantity != null && parsedQuantity > availableToSell

    val availableCash = if (type == InvestmentTransactionType.BUY && account != null) {
        availableCashToBuy(account, investmentsInAccount, date, excludingTransactionId = initial?.id)
    } else {
        null
    }
    val totalCost = if (parsedQuantity != null && parsedPrice != null) tradeValue + effectiveCommission else null
    val exceedsCash = availableCash != null && totalCost != null && totalCost > availableCash

    val isValid = parsedQuantity != null && parsedQuantity.signum() > 0 &&
        parsedPrice != null && parsedPrice.signum() > 0 && !commissionInvalid && !exceedsHoldings && !exceedsCash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit event" else "Add ${type.label.lowercase()}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing) {
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
                            InvestmentTransactionType.entries.forEach { option ->
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Date")
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantityText.isNotBlank() && (parsedQuantity == null || parsedQuantity.signum() <= 0 || exceedsHoldings),
                    supportingText = if (exceedsHoldings) {
                        { Text("Only ${availableToSell?.toPlainString()} available to sell as of this date") }
                    } else {
                        null
                    }
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price per unit (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceText.isNotBlank() && (parsedPrice == null || parsedPrice.signum() <= 0 || exceedsCash),
                    supportingText = if (exceedsCash) {
                        { Text("Only ${availableCash?.toPlainString()} ${currency.symbol} available in this account as of this date") }
                    } else {
                        null
                    }
                )
                OutlinedTextField(
                    value = commissionText,
                    onValueChange = { commissionText = it },
                    label = { Text("Commission (${currency.symbol})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = commissionInvalid,
                    supportingText = {
                        Text(
                            if (commissionText.isBlank() && suggestedCommission != null) {
                                "Defaults to ${suggestedCommission.toPlainString()} ${currency.symbol} from account settings"
                            } else {
                                "Optional — leave blank to use the account's default"
                            }
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(type, date, parsedQuantity!!, parsedPrice!!, effectiveCommission) },
                enabled = isValid
            ) { Text(if (isEditing) "Save" else "Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
