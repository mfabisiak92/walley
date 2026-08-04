package com.walley.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.walley.app.R
import com.walley.app.feature.lock.PIN_MAX_LENGTH
import com.walley.app.feature.lock.PIN_MIN_LENGTH
import com.walley.app.feature.lock.sanitizePinInput

@Composable
fun ChangePinDialog(
    hasError: Boolean,
    onCurrentPinChanged: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (currentPin: String, newPin: String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    val newPinValid = newPin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH
    val mismatch = confirmPin.isNotEmpty() && newPin != confirmPin
    val isValid = currentPin.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH && newPinValid && newPin == confirmPin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_change_pin)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        currentPin = sanitizePinInput(it)
                        onCurrentPinChanged()
                    },
                    label = { Text(stringResource(R.string.settings_current_pin)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = hasError,
                    supportingText = if (hasError) {
                        { Text(stringResource(R.string.settings_pin_incorrect)) }
                    } else {
                        null
                    }
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = sanitizePinInput(it) },
                    label = { Text(stringResource(R.string.settings_new_pin_hint, PIN_MIN_LENGTH, PIN_MAX_LENGTH)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = sanitizePinInput(it) },
                    label = { Text(stringResource(R.string.settings_confirm_new_pin)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = mismatch,
                    supportingText = if (mismatch) {
                        { Text(stringResource(R.string.settings_pins_dont_match)) }
                    } else {
                        null
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentPin, newPin) },
                enabled = isValid
            ) { Text(stringResource(R.string.settings_change)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
        }
    )
}
