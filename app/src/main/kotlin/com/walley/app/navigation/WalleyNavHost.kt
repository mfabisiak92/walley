package com.walley.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.feature.accounts.UpdateBalancesScreen
import com.walley.app.feature.analytics.AnalyticsScreen
import com.walley.app.feature.budget.AdHocBudgetDetailScreen
import com.walley.app.feature.budget.AdHocWizardScreen
import com.walley.app.feature.budget.BudgetDetailScreen
import com.walley.app.feature.budget.BudgetSettingsScreen
import com.walley.app.feature.budget.BudgetWizardScreen
import com.walley.app.feature.home.NetWorthDetailScreen
import com.walley.app.feature.investments.EquityDetailScreen
import com.walley.app.feature.investments.InvestmentDetailScreen
import com.walley.app.feature.investments.UpdatePricesScreen
import com.walley.app.feature.settings.SettingsScreen

private object WalleyDestinations {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val BUDGET_WIZARD = "budget_wizard?cloneFrom={cloneFromBudgetId}&resume={resumeBudgetId}"
    const val BUDGET_DETAIL = "budget_detail/{budgetId}"
    const val NET_WORTH_DETAIL = "net_worth_detail"
    const val ANALYTICS = "analytics"
    const val EQUITY_DETAIL = "equity_detail/{equityId}"
    const val INVESTMENT_DETAIL = "investment_detail/{investmentId}"
    const val AD_HOC_WIZARD = "ad_hoc_wizard"
    const val AD_HOC_DETAIL = "ad_hoc_detail/{adHocBudgetId}"
    const val UPDATE_PRICES = "update_prices"
    const val BUDGET_SETTINGS = "budget_settings/{budgetId}"
    const val UPDATE_BALANCES = "update_balances/{group}"

    fun budgetDetail(budgetId: Long) = "budget_detail/$budgetId"
    fun budgetSettings(budgetId: Long) = "budget_settings/$budgetId"
    fun updateBalances(group: AccountBalanceGroup) = "update_balances/${group.name}"
    fun budgetWizard(cloneFromBudgetId: Long? = null, resumeBudgetId: Long? = null): String {
        val params = listOfNotNull(
            cloneFromBudgetId?.let { "cloneFrom=$it" },
            resumeBudgetId?.let { "resume=$it" }
        )
        return if (params.isEmpty()) "budget_wizard" else "budget_wizard?" + params.joinToString("&")
    }
    fun equityDetail(equityId: Long) = "equity_detail/$equityId"
    fun investmentDetail(investmentId: Long) = "investment_detail/$investmentId"
    fun adHocDetail(adHocBudgetId: Long) = "ad_hoc_detail/$adHocBudgetId"
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
                onResumeBudgetDraft = { budgetId ->
                    navController.navigate(WalleyDestinations.budgetWizard(resumeBudgetId = budgetId))
                },
                onNavigateToNetWorthDetail = { navController.navigate(WalleyDestinations.NET_WORTH_DETAIL) },
                onNavigateToAnalytics = { navController.navigate(WalleyDestinations.ANALYTICS) },
                onOpenEquity = { equityId ->
                    navController.navigate(WalleyDestinations.equityDetail(equityId))
                },
                onOpenInvestment = { investmentId ->
                    navController.navigate(WalleyDestinations.investmentDetail(investmentId))
                },
                onNavigateToAdHocWizard = { navController.navigate(WalleyDestinations.AD_HOC_WIZARD) },
                onOpenAdHocBudget = { adHocBudgetId ->
                    navController.navigate(WalleyDestinations.adHocDetail(adHocBudgetId))
                },
                onOpenUpdatePrices = { navController.navigate(WalleyDestinations.UPDATE_PRICES) },
                onOpenUpdateBalances = { group ->
                    navController.navigate(WalleyDestinations.updateBalances(group))
                }
            )
        }
        composable(WalleyDestinations.NET_WORTH_DETAIL) {
            NetWorthDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(WalleyDestinations.ANALYTICS) {
            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
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
            arguments = listOf(
                navArgument("cloneFromBudgetId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("resumeBudgetId") { type = NavType.LongType; defaultValue = -1L }
            )
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
                },
                onOpenSettings = { budgetId ->
                    navController.navigate(WalleyDestinations.budgetSettings(budgetId))
                }
            )
        }
        composable(
            WalleyDestinations.BUDGET_SETTINGS,
            arguments = listOf(navArgument("budgetId") { type = NavType.LongType })
        ) {
            BudgetSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            WalleyDestinations.EQUITY_DETAIL,
            arguments = listOf(navArgument("equityId") { type = NavType.LongType })
        ) {
            EquityDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            WalleyDestinations.INVESTMENT_DETAIL,
            arguments = listOf(navArgument("investmentId") { type = NavType.LongType })
        ) {
            InvestmentDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(WalleyDestinations.AD_HOC_WIZARD) {
            AdHocWizardScreen(
                onDone = { adHocBudgetId ->
                    navController.navigate(WalleyDestinations.adHocDetail(adHocBudgetId)) {
                        popUpTo(WalleyDestinations.MAIN)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            WalleyDestinations.AD_HOC_DETAIL,
            arguments = listOf(navArgument("adHocBudgetId") { type = NavType.LongType })
        ) {
            AdHocBudgetDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(WalleyDestinations.UPDATE_PRICES) {
            UpdatePricesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            WalleyDestinations.UPDATE_BALANCES,
            arguments = listOf(navArgument("group") { type = NavType.StringType })
        ) {
            UpdateBalancesScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
