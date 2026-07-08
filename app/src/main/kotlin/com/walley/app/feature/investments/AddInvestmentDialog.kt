package com.walley.app.feature.investments

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
import androidx.compose.material3.MaterialTheme
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
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentDialog(
    investmentAccounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        ticker: String,
        category: InvestmentCategory,
        purchaseDate: LocalDate,
        quantity: BigDecimal,
        currency: Currency,
        price: BigDecimal,
        currentPrice: BigDecimal,
        accountId: Long
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ticker by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(InvestmentCategory.STOCK) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var purchaseDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var currentPriceText by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<Long?>(null) }
    var accountTouched by remember { mutableStateOf(false) }

    val selectedAccount = investmentAccounts.find { it.id == accountId }
    val parsedQuantity = quantityText.toBigDecimalOrNull()
    val parsedPrice = priceText.toBigDecimalOrNull()
    val parsedCurrentPrice = currentPriceText.toBigDecimalOrNull() ?: parsedPrice
    val isValid = name.isNotBlank() && ticker.isNotBlank() &&
        parsedQuantity != null && parsedPrice != null && selectedAccount != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add investment") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (investmentAccounts.isEmpty()) {
                    Text(
                        "You need an investment account before adding an investment. " +
                            "Create one from the Accounts screen first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it },
                    label = { Text("Ticker") },
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        InvestmentCategory.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    category = option
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Purchase date")
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(purchaseDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                }
                InvestmentAccountDropdown(
                    accounts = investmentAccounts,
                    selectedAccountId = accountId,
                    onAccountSelected = {
                        accountId = it
                        accountTouched = true
                    },
                    isError = accountTouched && selectedAccount == null
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantityText.isNotBlank() && parsedQuantity == null
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price per unit" + (selectedAccount?.let { " (${it.currency.symbol})" } ?: "")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceText.isNotBlank() && parsedPrice == null
                )
                OutlinedTextField(
                    value = currentPriceText,
                    onValueChange = { currentPriceText = it },
                    label = { Text("Current price (optional, defaults to price per unit)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = currentPriceText.isNotBlank() && currentPriceText.toBigDecimalOrNull() == null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name.trim(),
                        ticker.trim().uppercase(),
                        category,
                        purchaseDate,
                        parsedQuantity!!,
                        selectedAccount!!.currency,
                        parsedPrice!!,
                        parsedCurrentPrice!!,
                        selectedAccount.id
                    )
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
            initialSelectedDateMillis = purchaseDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            purchaseDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
