package com.walley.app.feature.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.R
import com.walley.app.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface BackupUiState {
    data object Ready : BackupUiState
    data object BackingUp : BackupUiState
    data object Restoring : BackupUiState
    data class Done(val message: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Ready)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onBackupFileSelected(fileUri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.BackingUp
            try {
                backupRepository.backupNow(fileUri)
                _uiState.value = BackupUiState.Done(context.getString(R.string.backup_completed_successfully))
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(context.getString(R.string.backup_failed, (e.message ?: e.toString())))
            }
        }
    }

    fun onRestoreFileSelected(fileUri: Uri) {
        viewModelScope.launch {
            _uiState.value = BackupUiState.Restoring
            try {
                backupRepository.restoreSnapshot(fileUri)
                _uiState.value = BackupUiState.Done(context.getString(R.string.backup_restored_successfully))
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error(context.getString(R.string.backup_restore_failed, (e.message ?: e.toString())))
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = BackupUiState.Ready
    }

    fun getLastBackupAt(): Instant? {
        // This will be called in a LaunchedEffect, so we get the current value synchronously
        // The flow is collected separately by the UI
        return null // Actual value comes from the flow in BackupScreen
    }
}
