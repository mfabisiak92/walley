package com.walley.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.walley.app.feature.budget.BudgetDetailScreen
import com.walley.app.feature.budget.BudgetWizardScreen
import com.walley.app.feature.settings.SettingsScreen

private object WalleyDestinations {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val BUDGET_WIZARD = "budget_wizard"
    const val BUDGET_DETAIL = "budget_detail/{budgetId}"

    fun budgetDetail(budgetId: Long) = "budget_detail/$budgetId"
}

@Composable
fun WalleyNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = WalleyDestinations.MAIN) {
        composable(WalleyDestinations.MAIN) {
            MainTabsScreen(
                onNavigateToSettings = { navController.navigate(WalleyDestinations.SETTINGS) },
                onNavigateToBudgetWizard = { navController.navigate(WalleyDestinations.BUDGET_WIZARD) },
                onNavigateToBudgetDetail = { budgetId ->
                    navController.navigate(WalleyDestinations.budgetDetail(budgetId))
                }
            )
        }
        composable(WalleyDestinations.SETTINGS) {
            SettingsScreen(
                onNavigateHome = {
                    navController.popBackStack(WalleyDestinations.MAIN, inclusive = false)
                }
            )
        }
        composable(WalleyDestinations.BUDGET_WIZARD) {
            BudgetWizardScreen(
                onDone = { budgetId ->
                    navController.navigate(WalleyDestinations.budgetDetail(budgetId)) {
                        popUpTo(WalleyDestinations.MAIN)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            WalleyDestinations.BUDGET_DETAIL,
            arguments = listOf(navArgument("budgetId") { type = NavType.LongType })
        ) {
            BudgetDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
