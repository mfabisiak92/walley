package com.walley.app.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.R
import com.walley.app.data.remote.DriveFile
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timestampFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        result.data?.let { viewModel.onConsentResult(it) }
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
            when (val current = state) {
                is BackupUiState.SignedOut -> SignedOutContent(onSignIn = { viewModel.signIn(context) })
                is BackupUiState.Authorizing -> LoadingContent(stringResource(R.string.backup_connecting))
                is BackupUiState.NeedsConsent -> {
                    LoadingContent(stringResource(R.string.backup_waiting_for_consent))
                    LaunchedEffect(current.pendingIntent) {
                        consentLauncher.launch(IntentSenderRequest.Builder(current.pendingIntent.intentSender).build())
                    }
                }
                is BackupUiState.BackingUp -> LoadingContent(stringResource(R.string.backup_backing_up))
                is BackupUiState.Restoring -> LoadingContent(stringResource(R.string.backup_restoring))
                is BackupUiState.SignedIn -> SignedInContent(
                    state = current,
                    onBackupNow = viewModel::backupNow,
                    onRestore = viewModel::requestRestore,
                    onAutoBackupToggle = viewModel::setAutoBackupEnabled
                )
                is BackupUiState.ConfirmRestore -> {
                    // Content behind the dialog: whatever was showing before still shows through.
                    ConfirmRestoreDialog(
                        file = current.file,
                        onConfirm = { viewModel.confirmRestore(current.file) },
                        onDismiss = viewModel::cancelRestore
                    )
                }
                is BackupUiState.Done -> MessageContent(current.message, onDismiss = viewModel::dismissMessage)
                is BackupUiState.Error -> MessageContent(current.message, isError = true, onDismiss = viewModel::dismissMessage)
            }
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
        Button(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) { Text(stringResource(R.string.backup_ok)) }
    }
}

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.backup_sign_in_description),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = onSignIn) { Text(stringResource(R.string.backup_sign_in_with_google)) }
    }
}

@Composable
private fun SignedInContent(
    state: BackupUiState.SignedIn,
    onBackupNow: () -> Unit,
    onRestore: (DriveFile) -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.accountEmail?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val lastBackupText = state.lastBackupAt?.let {
            stringResource(R.string.backup_last_backup, timestampFormatter.format(it.atZone(ZoneId.systemDefault())))
        } ?: stringResource(R.string.backup_no_backup_yet)
        Text(lastBackupText, style = MaterialTheme.typography.bodyMedium)

        Button(onClick = onBackupNow, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.backup_back_up_now)) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.backup_auto_daily), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.autoBackupEnabled, onCheckedChange = onAutoBackupToggle)
        }

        Text(stringResource(R.string.backup_previous_backups), style = MaterialTheme.typography.titleMedium)

        if (state.snapshots.isEmpty()) {
            Text(
                stringResource(R.string.backup_no_backups_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.snapshots, key = { it.id }) { file ->
                    Card(
                        onClick = { onRestore(file) },
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(file.createdTime ?: file.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.backup_tap_to_restore),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmRestoreDialog(file: DriveFile, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_dialog_title)) },
        text = {
            Text(stringResource(R.string.backup_restore_dialog_text))
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.backup_restore_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.backup_cancel)) } }
    )
}
