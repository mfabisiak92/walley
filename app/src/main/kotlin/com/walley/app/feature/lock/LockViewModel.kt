package com.walley.app.feature.lock

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How long the app can sit in the background before it re-locks itself. */
private const val LOCK_GRACE_PERIOD_MS = 30_000L

@HiltViewModel
class LockViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    // null while the stored preference is still loading
    val pinSet: StateFlow<Boolean?> = securityRepository.observePinSet()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fingerprintEnabled: StateFlow<Boolean> = securityRepository.observeFingerprintUnlock()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError.asStateFlow()

    // Set when the app leaves the foreground; cleared once checked on the way back in.
    private var backgroundedAtElapsedMillis: Long? = null

    /** Call when the app leaves the foreground — starts the grace-period clock, doesn't lock yet. */
    fun onAppBackgrounded() {
        backgroundedAtElapsedMillis = SystemClock.elapsedRealtime()
    }

    /** Call when the app returns to the foreground — locks only if the grace period has elapsed. */
    fun onAppForegrounded() {
        val backgroundedAt = backgroundedAtElapsedMillis ?: return
        backgroundedAtElapsedMillis = null
        val elapsed = SystemClock.elapsedRealtime() - backgroundedAt
        if (elapsed >= LOCK_GRACE_PERIOD_MS) {
            _unlocked.value = false
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            securityRepository.setPin(pin)
            _unlocked.value = true
        }
    }

    fun unlockWithPin(pin: String) {
        viewModelScope.launch {
            if (securityRepository.verifyPin(pin)) {
                _pinError.value = false
                _unlocked.value = true
            } else {
                _pinError.value = true
            }
        }
    }

    fun clearPinError() {
        _pinError.value = false
    }

    fun unlockWithBiometrics() {
        _pinError.value = false
        _unlocked.value = true
    }
}
