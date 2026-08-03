package com.walley.app.feature.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.remote.DriveFile
import com.walley.app.data.repository.BackupRepository
import com.walley.app.work.BackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface BackupUiState {
    data object SignedOut : BackupUiState
    data object Authorizing : BackupUiState
    /** Interactive consent is needed for the `drive.appdata` scope — the UI must launch [pendingIntent]. */
    data class NeedsConsent(val pendingIntent: PendingIntent) : BackupUiState
    data class SignedIn(
        val accountEmail: String?,
        val snapshots: List<DriveFile>,
        val lastBackupAt: Instant?,
        val autoBackupEnabled: Boolean
    ) : BackupUiState
    data object BackingUp : BackupUiState
    data class ConfirmRestore(val file: DriveFile) : BackupUiState
    data object Restoring : BackupUiState
    data class Done(val message: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val backupScheduler: BackupScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.SignedOut)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    // Short-lived Drive access token for this screen session; re-derived via authorize() rather
    // than persisted, since the Authorization API is designed to be re-called freely.
    private var cachedAccessToken: String? = null

    init {
        viewModelScope.launch {
            val email = backupRepository.observeSignedInAccountEmail().first()
            if (email != null) authorize() else _uiState.value = BackupUiState.SignedOut
        }
    }

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Authorizing
            try {
                backupRepository.signIn(activityContext)
                authorize()
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Sign-in failed: ${e.message}")
            }
        }
    }

    private suspend fun authorize() {
        _uiState.value = BackupUiState.Authorizing
        try {
            val result = backupRepository.requestDriveAuthorization()
            val token = result.accessToken
            if (result.hasResolution() || token == null) {
                val pendingIntent = result.pendingIntent
                _uiState.value = if (pendingIntent != null) {
                    BackupUiState.NeedsConsent(pendingIntent)
                } else {
                    BackupUiState.Error("Couldn't get Drive access — try signing in again.")
                }
                return
            }
            cachedAccessToken = token
            loadSignedInState(token)
        } catch (e: Exception) {
            _uiState.value = BackupUiState.Error("Couldn't reach Google Drive: ${e.message}")
        }
    }

    fun onConsentResult(intent: Intent) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Authorizing
            try {
                val result = backupRepository.authorizationResultFromIntent(intent)
                val token = result.accessToken
                if (token == null) {
                    _uiState.value = BackupUiState.Error("Drive access wasn't granted.")
                    return@launch
                }
                cachedAccessToken = token
                loadSignedInState(token)
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Couldn't finish authorizing: ${e.message}")
            }
        }
    }

    fun backupNow() {
        val token = cachedAccessToken ?: return
        viewModelScope.launch {
            _uiState.value = BackupUiState.BackingUp
            try {
                backupRepository.backupNow(token)
                loadSignedInState(token)
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Backup failed: ${e.message}")
            }
        }
    }

    fun requestRestore(file: DriveFile) {
        _uiState.value = BackupUiState.ConfirmRestore(file)
    }

    fun cancelRestore() {
        viewModelScope.launch { refreshOrSignOut() }
    }

    fun confirmRestore(file: DriveFile) {
        val token = cachedAccessToken ?: return
        viewModelScope.launch {
            _uiState.value = BackupUiState.Restoring
            try {
                backupRepository.restoreSnapshot(token, file.id)
                _uiState.value = BackupUiState.Done("Restored from ${file.name}")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Restore failed: ${e.message}")
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupRepository.setAutoBackupEnabled(enabled)
            if (enabled) backupScheduler.enable() else backupScheduler.disable()
            cachedAccessToken?.let { loadSignedInState(it) }
        }
    }

    fun dismissMessage() {
        viewModelScope.launch { refreshOrSignOut() }
    }

    private suspend fun refreshOrSignOut() {
        val token = cachedAccessToken
        if (token != null) loadSignedInState(token) else _uiState.value = BackupUiState.SignedOut
    }

    private suspend fun loadSignedInState(accessToken: String) {
        val email = backupRepository.observeSignedInAccountEmail().first()
        val snapshots = backupRepository.listSnapshots(accessToken)
        val lastBackupAt = backupRepository.observeLastBackupAt().first()
        val autoBackupEnabled = backupRepository.observeAutoBackupEnabled().first()
        _uiState.value = BackupUiState.SignedIn(email, snapshots, lastBackupAt, autoBackupEnabled)
    }
}
