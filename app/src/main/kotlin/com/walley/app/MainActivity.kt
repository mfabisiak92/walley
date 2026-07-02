package com.walley.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.feature.settings.SettingsViewModel
import com.walley.app.navigation.WalleyNavHost
import com.walley.app.ui.theme.WalleyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val darkModeOverride by settingsViewModel.darkModeOverride.collectAsStateWithLifecycle()
            val useDarkTheme = darkModeOverride ?: isSystemInDarkTheme()

            WalleyTheme(darkTheme = useDarkTheme) {
                WalleyNavHost()
            }
        }
    }
}
