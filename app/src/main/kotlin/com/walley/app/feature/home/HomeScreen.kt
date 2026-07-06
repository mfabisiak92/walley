package com.walley.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.CurrencyTotal

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAccounts: () -> Unit,
    onNavigateToInvestments: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val homeBalances by viewModel.homeBalances.collectAsStateWithLifecycle()
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            WalleyTopBar(
                onTitleClick = {},
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            netWorth?.let { NetWorthCard(it) }
            TotalBalanceCard(currencyTotals = homeBalances.total)
            if (homeBalances.savings.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SubBalanceCard(
                        modifier = Modifier.weight(1f),
                        title = "Available",
                        currencyTotals = homeBalances.available,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    SubBalanceCard(
                        modifier = Modifier.weight(1f),
                        title = "Savings",
                        currencyTotals = homeBalances.savings,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ModuleTile(
                    title = "Accounts",
                    icon = Icons.Default.AccountBox,
                    onClick = onNavigateToAccounts
                )
                ModuleTile(
                    title = "Investments",
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = onNavigateToInvestments
                )
            }
        }
    }
}

@Composable
private fun NetWorthCard(netWorth: NetWorthState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Net worth", style = MaterialTheme.typography.titleMedium)
            if (netWorth.amount != null) {
                Text(
                    text = formatMoney(netWorth.amount, netWorth.currency),
                    style = MaterialTheme.typography.headlineLarge
                )
                netWorth.rateDate?.let { date ->
                    Text(
                        text = "ECB rates · $date",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(
                    text = "Exchange rates unavailable",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(currencyTotals: List<CurrencyTotal>) {
    if (currencyTotals.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Total balance", style = MaterialTheme.typography.titleMedium)
            currencyTotals.forEach { currencyTotal ->
                Text(
                    text = formatMoney(currencyTotal.total, currencyTotal.currency),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
private fun SubBalanceCard(
    modifier: Modifier = Modifier,
    title: String,
    currencyTotals: List<CurrencyTotal>,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            if (currencyTotals.isEmpty()) {
                Text("—", style = MaterialTheme.typography.titleMedium)
            } else {
                currencyTotals.forEach { currencyTotal ->
                    Text(
                        text = formatMoney(currencyTotal.total, currencyTotal.currency),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
