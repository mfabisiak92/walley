package com.walley.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.walley.app.feature.settings.SettingsScreen

private object WalleyDestinations {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun WalleyNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = WalleyDestinations.MAIN) {
        composable(WalleyDestinations.MAIN) {
            MainTabsScreen(
                onNavigateToSettings = { navController.navigate(WalleyDestinations.SETTINGS) }
            )
        }
        composable(WalleyDestinations.SETTINGS) {
            SettingsScreen(
                onNavigateHome = {
                    navController.popBackStack(WalleyDestinations.MAIN, inclusive = false)
                }
            )
        }
    }
}
