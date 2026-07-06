package com.walley.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.feature.lock.AppLockGate
import com.walley.app.feature.lock.LockViewModel
import com.walley.app.feature.settings.SettingsViewModel
import com.walley.app.navigation.WalleyNavHost
import com.walley.app.ui.theme.WalleyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val lockViewModel: LockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val darkModeOverride by settingsViewModel.darkModeOverride.collectAsStateWithLifecycle()
            val useDarkTheme = darkModeOverride ?: isSystemInDarkTheme()

            WalleyTheme(darkTheme = useDarkTheme) {
                AppLockGate(viewModel = lockViewModel) {
                    WalleyNavHost()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Require authentication again after the app leaves the foreground,
        // but not for configuration changes like rotation.
        if (!isChangingConfigurations) {
            lockViewModel.lock()
        }
    }
}
