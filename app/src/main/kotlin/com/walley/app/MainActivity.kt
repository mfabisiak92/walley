package com.walley.app

import android.os.Bundle
import android.view.WindowManager
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
        // Financial data must never leak into the recent-apps screenshot, since the app can now
        // stay unlocked for a short grace period after being backgrounded (see onStop/onStart).
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
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

    override fun onStart() {
        super.onStart()
        lockViewModel.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        // Start the grace-period clock when the app leaves the foreground, but not for
        // configuration changes like rotation — a brief return (e.g. switching apps) won't
        // require unlocking again; only leaving it in the background past the grace period will.
        if (!isChangingConfigurations) {
            lockViewModel.onAppBackgrounded()
        }
    }
}
