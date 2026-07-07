package com.walley.app.feature.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLiabilityDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        currency: Currency,
        originalAmount: BigDecimal,
        currentBalance: BigDecimal,
        startDate: LocalDate
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currency.PLN) }
    var originalAmountText by remember { mutableStateOf("") }
    var currentBalanceText by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var currencyMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedOriginalAmount = originalAmountText.toBigDecimalOrNull()
    val parsedCurrentBalance = currentBalanceText.toBigDecimalOrNull() ?: parsedOriginalAmount
    val isValid = name.isNotBlank() && parsedOriginalAmount != null && parsedCurrentBalance != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add liability") },
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
                OutlinedTextField(
                    value = originalAmountText,
                    onValueChange = { originalAmountText = it },
                    label = { Text("Original amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = originalAmountText.isNotBlank() && parsedOriginalAmount == null
                )
                OutlinedTextField(
                    value = currentBalanceText,
                    onValueChange = { currentBalanceText = it },
                    label = { Text("Current balance (optional, defaults to original amount)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = currentBalanceText.isNotBlank() && currentBalanceText.toBigDecimalOrNull() == null
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start date")
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name.trim(), currency, parsedOriginalAmount!!, parsedCurrentBalance!!, startDate)
                },
                enabled = isValid
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
