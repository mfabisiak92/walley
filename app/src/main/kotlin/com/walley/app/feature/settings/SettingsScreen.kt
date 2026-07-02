package com.walley.app.feature.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.ui.WalleyTopBar

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
    val isDarkMode = darkModeOverride ?: isSystemInDarkTheme()

    Scaffold(
        modifier = modifier,
        topBar = { WalleyTopBar(onTitleClick = onNavigateHome) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isDarkMode, onCheckedChange = viewModel::setDarkMode)
            }
        }
    }
}
