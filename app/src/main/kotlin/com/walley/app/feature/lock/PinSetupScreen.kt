package com.walley.app.feature.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walley.app.R

const val PIN_MIN_LENGTH = 4
const val PIN_MAX_LENGTH = 6

fun sanitizePinInput(input: String): String = input.filter { it.isDigit() }.take(PIN_MAX_LENGTH)

@Composable
fun PinSetupScreen(
    modifier: Modifier = Modifier,
    onPinConfirmed: (String) -> Unit
) {
    var firstPin by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    val confirming = firstPin != null
    val inputValid = pinInput.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (confirming) {
                    stringResource(R.string.lock_setup_confirm_title)
                } else {
                    stringResource(R.string.lock_setup_title)
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = if (confirming) {
                    stringResource(R.string.lock_setup_confirm_body)
                } else {
                    stringResource(R.string.lock_setup_body, PIN_MIN_LENGTH, PIN_MAX_LENGTH)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = pinInput,
                onValueChange = {
                    pinInput = sanitizePinInput(it)
                    mismatch = false
                },
                label = { Text(stringResource(R.string.lock_pin_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                isError = mismatch,
                supportingText = if (mismatch) {
                    { Text(stringResource(R.string.lock_setup_pin_mismatch)) }
                } else null
            )
            Button(
                onClick = {
                    val current = firstPin
                    when {
                        current == null -> {
                            firstPin = pinInput
                            pinInput = ""
                        }
                        current == pinInput -> onPinConfirmed(current)
                        else -> {
                            firstPin = null
                            pinInput = ""
                            mismatch = true
                        }
                    }
                },
                enabled = inputValid
            ) {
                Text(
                    if (confirming) {
                        stringResource(R.string.lock_setup_confirm_button)
                    } else {
                        stringResource(R.string.lock_setup_continue_button)
                    }
                )
            }
        }
    }
}
