package com.walley.app.feature.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AppLockGate(
    viewModel: LockViewModel,
    content: @Composable () -> Unit
) {
    val pinSet by viewModel.pinSet.collectAsStateWithLifecycle()
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()

    when {
        pinSet == null -> Unit // preference still loading; avoid flashing any screen
        pinSet == false -> PinSetupScreen(onPinConfirmed = viewModel::setPin)
        !unlocked -> LockScreen(viewModel = viewModel)
        else -> content()
    }
}
