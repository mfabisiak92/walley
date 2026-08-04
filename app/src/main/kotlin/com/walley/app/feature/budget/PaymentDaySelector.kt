package com.walley.app.feature.budget

import com.walley.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private enum class PaymentDayMode { NONE, SPECIFIC_DAY, LAST_DAY }

@Composable
fun PaymentDaySelector(
    paymentDay: Int?,
    paymentDayIsLastOfMonth: Boolean,
    onChange: (day: Int?, isLastOfMonth: Boolean) -> Unit
) {
    val mode = when {
        paymentDayIsLastOfMonth -> PaymentDayMode.LAST_DAY
        paymentDay != null -> PaymentDayMode.SPECIFIC_DAY
        else -> PaymentDayMode.NONE
    }

    Column {
        Text(stringResource(R.string.budget_payment_day_optional))
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == PaymentDayMode.NONE,
                onClick = { onChange(null, false) },
                label = { Text(stringResource(R.string.budget_none)) }
            )
            FilterChip(
                selected = mode == PaymentDayMode.SPECIFIC_DAY,
                onClick = { onChange(paymentDay ?: 1, false) },
                label = { Text(stringResource(R.string.budget_day_of_month_option)) }
            )
            FilterChip(
                selected = mode == PaymentDayMode.LAST_DAY,
                onClick = { onChange(null, true) },
                label = { Text(stringResource(R.string.budget_last_day_option)) }
            )
        }
        if (mode == PaymentDayMode.SPECIFIC_DAY) {
            OutlinedTextField(
                value = (paymentDay ?: 1).toString(),
                onValueChange = { text ->
                    val day = text.toIntOrNull()?.coerceIn(1, 31)
                    if (day != null) onChange(day, false)
                },
                label = { Text(stringResource(R.string.budget_day_range_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(120.dp)
            )
        }
    }
}
