package com.walley.app.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timestampFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

// Colon-free and space-free so it's a valid filename on every filesystem the document picker might
// save to, while still sorting chronologically as plain text.
private val backupFileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lastBackupAt by viewModel.backupRepository.observeLastBackupAt().collectAsStateWithLifecycle(null)

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let { viewModel.onBackupFileSelected(it) }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onRestoreFileSelected(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.backup_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (state) {
                is BackupUiState.Ready -> ReadyContent(
                    lastBackupAt = lastBackupAt,
                    onBackupClick = {
                        val timestamp = LocalDateTime.now().format(backupFileNameFormatter)
                        createDocumentLauncher.launch("walley_backup_$timestamp.enc")
                    },
                    onRestoreClick = { openDocumentLauncher.launch(arrayOf("*/*")) }
                )
                is BackupUiState.BackingUp -> LoadingContent(stringResource(R.string.backup_backing_up))
                is BackupUiState.Restoring -> LoadingContent(stringResource(R.string.backup_restoring))
                is BackupUiState.Done -> MessageContent((state as BackupUiState.Done).message, onDismiss = viewModel::dismissMessage)
                is BackupUiState.Error -> MessageContent((state as BackupUiState.Error).message, isError = true, onDismiss = viewModel::dismissMessage)
            }
        }
    }
}

@Composable
private fun ReadyContent(
    lastBackupAt: Instant?,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.backup_manual_description),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val lastBackupText = lastBackupAt?.let {
            stringResource(R.string.backup_last_backup, timestampFormatter.format(it.atZone(ZoneId.systemDefault())))
        } ?: stringResource(R.string.backup_no_backup_yet)
        Text(lastBackupText, style = MaterialTheme.typography.bodyMedium)

        Button(onClick = onBackupClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.backup_back_up_now))
        }

        Button(onClick = onRestoreClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.backup_restore_from_file))
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun MessageContent(message: String, isError: Boolean = false, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.backup_ok))
        }
    }
}
