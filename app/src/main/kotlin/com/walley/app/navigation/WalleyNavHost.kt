package com.walley.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.walley.app.feature.accounts.AccountsScreen
import com.walley.app.feature.home.HomeScreen
import com.walley.app.feature.settings.SettingsScreen

private object WalleyDestinations {
    const val HOME = "home"
    const val ACCOUNTS = "accounts"
    const val SETTINGS = "settings"
}

@Composable
fun WalleyNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = WalleyDestinations.HOME) {
        composable(WalleyDestinations.HOME) {
            HomeScreen(
                onNavigateToAccounts = { navController.navigate(WalleyDestinations.ACCOUNTS) },
                onNavigateToSettings = { navController.navigate(WalleyDestinations.SETTINGS) }
            )
        }
        composable(WalleyDestinations.ACCOUNTS) {
            AccountsScreen(
                onNavigateHome = {
                    navController.popBackStack(WalleyDestinations.HOME, inclusive = false)
                }
            )
        }
        composable(WalleyDestinations.SETTINGS) {
            SettingsScreen(
                onNavigateHome = {
                    navController.popBackStack(WalleyDestinations.HOME, inclusive = false)
                }
            )
        }
    }
}
