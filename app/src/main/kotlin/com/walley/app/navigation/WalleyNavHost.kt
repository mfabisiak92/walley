package com.walley.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.walley.app.feature.budget.BudgetDetailScreen
import com.walley.app.feature.budget.BudgetWizardScreen
import com.walley.app.feature.home.NetWorthDetailScreen
import com.walley.app.feature.settings.SettingsScreen

private object WalleyDestinations {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val BUDGET_WIZARD = "budget_wizard?cloneFrom={cloneFromBudgetId}"
    const val BUDGET_DETAIL = "budget_detail/{budgetId}"
    const val NET_WORTH_DETAIL = "net_worth_detail"

    fun budgetDetail(budgetId: Long) = "budget_detail/$budgetId"
    fun budgetWizard(cloneFromBudgetId: Long? = null) =
        if (cloneFromBudgetId != null) "budget_wizard?cloneFrom=$cloneFromBudgetId" else "budget_wizard"
}

@Composable
fun WalleyNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = WalleyDestinations.MAIN) {
        composable(WalleyDestinations.MAIN) {
            MainTabsScreen(
                onNavigateToSettings = { navController.navigate(WalleyDestinations.SETTINGS) },
                onNavigateToBudgetWizard = { navController.navigate(WalleyDestinations.budgetWizard()) },
                onNavigateToBudgetDetail = { budgetId ->
                    navController.navigate(WalleyDestinations.budgetDetail(budgetId))
                },
                onNavigateToNetWorthDetail = { navController.navigate(WalleyDestinations.NET_WORTH_DETAIL) }
            )
        }
        composable(WalleyDestinations.NET_WORTH_DETAIL) {
            NetWorthDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(WalleyDestinations.SETTINGS) {
            SettingsScreen(
                onNavigateHome = {
                    navController.popBackStack(WalleyDestinations.MAIN, inclusive = false)
                }
            )
        }
        composable(
            WalleyDestinations.BUDGET_WIZARD,
            arguments = listOf(navArgument("cloneFromBudgetId") { type = NavType.LongType; defaultValue = -1L })
        ) {
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
            BudgetDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onCloneBudget = { budgetId ->
                    navController.navigate(WalleyDestinations.budgetWizard(budgetId))
                }
            )
        }
    }
}
