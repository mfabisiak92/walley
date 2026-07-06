package com.walley.app.feature.lock

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

    fun lock() {
        _unlocked.value = false
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
