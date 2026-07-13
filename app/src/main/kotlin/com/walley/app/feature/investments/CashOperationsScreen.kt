package com.walley.app.feature.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountOperation
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.netRealizedGain
import com.walley.app.domain.model.realizedGain
import com.walley.app.domain.model.realizedGainTax
import java.math.BigDecimal
import kotlinx.coroutines.launch

private val TABS = listOf("Details", "Operations")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashOperationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CashOperationsViewModel = hiltViewModel()
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val operations by viewModel.operations.collectAsStateWithLifecycle()
    val investmentsInAccount by viewModel.investmentsInAccount.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.let { "Cash operations · ${it.name}" } ?: "Cash operations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                TABS.forEachIndexed { index, label ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(label) }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    DetailsTab(account = account, operations = operations, investmentsInAccount = investmentsInAccount)
                } else {
                    OperationsTab(operations = operations, currency = account?.currency ?: Currency.PLN)
                }
            }
        }
    }
}

@Composable
private fun OperationsTab(operations: List<AccountOperation>, currency: Currency) {
    if (operations.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No cash operations imported yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(operations, key = { _, operation -> operation.id }) { index, operation ->
                if (index > 0) HorizontalDivider()
                CashOperationRow(operation, currency = currency)
            }
        }
    }
}

@Composable
private fun CashOperationRow(operation: AccountOperation, currency: Currency) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                operation.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                operation.date.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val isPositive = operation.amount.signum() >= 0
        Text(
            formatMoney(operation.amount, currency),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun DetailsTab(
    account: Account?,
    operations: List<AccountOperation>,
    investmentsInAccount: List<InvestmentWithTransactions>
) {
    if (account == null) return
    val currency = account.currency
    val netDeposits = operations.fold(BigDecimal.ZERO) { acc, operation -> acc + operation.amount }
    val realizedGainAmount = account.realizedGain(investmentsInAccount)
    val realizedTax = account.realizedGainTax(investmentsInAccount) ?: BigDecimal.ZERO
    val netRealizedGainAmount = account.netRealizedGain(investmentsInAccount)
    val unrealizedGainAmount = account.investmentGainLoss
    val netUnrealizedGainAmount = account.investmentNetProfit ?: unrealizedGainAmount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        val rows = listOf(
            "Current account balance" to account.balance,
            "Net deposits" to netDeposits,
            "Cost basis" to account.investmentCostBasis,
            "Uninvested cash" to account.uninvestedCash,
            "Realized gain" to realizedGainAmount,
            "Net realized gain" to netRealizedGainAmount,
            "Unrealized gain" to unrealizedGainAmount,
            "Net unrealized gain" to netUnrealizedGainAmount
        )
        itemsIndexed(rows, key = { _, (label, _) -> label }) { index, (label, value) ->
            if (index > 0) HorizontalDivider()
            DetailRow(label = label, value = value, currency = currency)
        }
        if (realizedTax.signum() > 0) {
            item { HorizontalDivider() }
            item {
                DetailRow(
                    label = "Tax on realized gain (${account.taxRate.label})",
                    value = realizedTax,
                    currency = currency,
                    negative = true
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: BigDecimal, currency: Currency, negative: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        val displayValue = if (negative) value.negate() else value
        Text(
            formatMoney(displayValue, currency),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                displayValue.signum() > 0 -> MaterialTheme.colorScheme.primary
                displayValue.signum() < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
