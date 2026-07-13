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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.biometric.biometricsAvailable
import com.walley.app.core.biometric.promptBiometrics

@Composable
fun LockScreen(
    modifier: Modifier = Modifier,
    viewModel: LockViewModel
) {
    val fingerprintEnabled by viewModel.fingerprintEnabled.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    var pinInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricsAvailable = remember { biometricsAvailable(context) }
    val showFingerprint = fingerprintEnabled && biometricsAvailable && activity != null

    fun promptBiometrics() {
        promptBiometrics(
            activity = activity!!,
            context = context,
            title = "Unlock Walley",
            negativeButtonText = "Use PIN",
            onSuccess = viewModel::unlockWithBiometrics
        )
    }

    LaunchedEffect(showFingerprint) {
        if (showFingerprint) promptBiometrics()
    }

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
            Text("Walley is locked", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = pinInput,
                onValueChange = {
                    pinInput = sanitizePinInput(it)
                    viewModel.clearPinError()
                },
                label = { Text("PIN") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                isError = pinError,
                supportingText = if (pinError) {
                    { Text("Wrong PIN — try again.") }
                } else null
            )
            Button(
                onClick = {
                    viewModel.unlockWithPin(pinInput)
                    pinInput = ""
                },
                enabled = pinInput.length in PIN_MIN_LENGTH..PIN_MAX_LENGTH
            ) {
                Text("Unlock")
            }
            if (showFingerprint) {
                TextButton(onClick = { promptBiometrics() }) {
                    Text("Use fingerprint")
                }
            }
        }
    }
}
