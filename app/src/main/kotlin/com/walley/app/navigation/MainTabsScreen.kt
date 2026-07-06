package com.walley.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.walley.app.feature.accounts.AccountsScreen
import com.walley.app.feature.assets.AssetsScreen
import com.walley.app.feature.budget.BudgetsScreen
import com.walley.app.feature.home.HomeScreen
import com.walley.app.feature.investments.InvestmentsScreen
import kotlinx.coroutines.launch

private val TabRed = Color(0xFFD32F2F)

private data class Tab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("Home", Icons.Filled.Home),
    Tab("Accounts", Icons.Filled.AccountBalance),
    Tab("Budget", Icons.Filled.Calculate),
    Tab("Investments", Icons.Filled.PieChart),
    Tab("Assets", Icons.Filled.Lock)
)

@Composable
fun MainTabsScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToBudgetWizard: () -> Unit,
    onNavigateToBudgetDetail: (Long) -> Unit,
    onNavigateToNetWorthDetail: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TabRed,
                            selectedTextColor = TabRed,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val goHome: () -> Unit = { scope.launch { pagerState.animateScrollToPage(0) } }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToNetWorthDetail = onNavigateToNetWorthDetail,
                    onNavigateToAnalytics = onNavigateToAnalytics
                )
                1 -> AccountsScreen(onNavigateHome = goHome)
                2 -> BudgetsScreen(
                    onNavigateHome = goHome,
                    onCreateBudget = onNavigateToBudgetWizard,
                    onOpenBudget = onNavigateToBudgetDetail
                )
                3 -> InvestmentsScreen(onNavigateHome = goHome)
                4 -> AssetsScreen(onNavigateHome = goHome)
            }
        }
    }
}
