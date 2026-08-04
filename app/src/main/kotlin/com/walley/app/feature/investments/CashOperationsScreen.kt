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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountOperation
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.displayName
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.estimatedTaxForYear
import com.walley.app.domain.model.netRealizedGain
import com.walley.app.domain.model.realizedGain
import com.walley.app.domain.model.realizedGainTax
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashOperationsScreen(
    onNavigateBack: () -> Unit,
    onOpenInvestment: (Long) -> Unit,
    viewModel: CashOperationsViewModel = hiltViewModel()
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val operations by viewModel.operations.collectAsStateWithLifecycle()
    val investmentsInAccount by viewModel.investmentsInAccount.collectAsStateWithLifecycle()
    val isInvestmentAccount = account?.type == AccountType.INVESTMENT
    val tabs = if (isInvestmentAccount) {
        listOf(
            stringResource(R.string.investments_tab_details),
            stringResource(R.string.investments_tab_investments),
            stringResource(R.string.investments_tab_operations)
        )
    } else {
        listOf(stringResource(R.string.investments_tab_operations))
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.investments_cd_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (tabs.size > 1) {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(label) }
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (isInvestmentAccount) {
                    when (page) {
                        0 -> DetailsTab(account = account, operations = operations, investmentsInAccount = investmentsInAccount)
                        1 -> InvestmentsTab(investmentsInAccount = investmentsInAccount, onOpenInvestment = onOpenInvestment)
                        else -> OperationsTab(operations = operations, currency = account?.currency ?: Currency.PLN)
                    }
                } else {
                    OperationsTab(operations = operations, currency = account?.currency ?: Currency.PLN)
                }
            }
        }
    }
}

@Composable
private fun InvestmentsTab(investmentsInAccount: List<InvestmentWithTransactions>, onOpenInvestment: (Long) -> Unit) {
    val openInvestments = investmentsInAccount.filter { it.quantity.signum() != 0 }
    if (openInvestments.isEmpty()) {
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
                stringResource(R.string.investments_empty_linked_investments),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(openInvestments, key = { it.investment.id }) { investment ->
                InvestmentRow(
                    data = investment,
                    accountName = null,
                    strategy = null,
                    onClick = { onOpenInvestment(investment.investment.id) },
                    onLongClick = {},
                    onClickStrategy = {}
                )
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
                stringResource(R.string.investments_empty_cash_operations),
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
    val currentYear = LocalDate.now().year
    val netDeposits = operations.fold(BigDecimal.ZERO) { acc, operation -> acc + operation.amount }
    val unrealizedGainAmount = account.investmentGainLoss
    val netUnrealizedGainAmount = account.investmentNetProfit ?: unrealizedGainAmount
    val realizedGainThisYear = investmentsInAccount.fold(BigDecimal.ZERO) { acc, data -> acc + data.realizedGainLossInYear(currentYear) }
    val realizedTaxThisYear = account.estimatedTaxForYear(investmentsInAccount, currentYear) ?: BigDecimal.ZERO
    val netRealizedGainThisYear = realizedGainThisYear - realizedTaxThisYear
    val realizedGainAmount = account.realizedGain(investmentsInAccount)
    val realizedTax = account.realizedGainTax(investmentsInAccount) ?: BigDecimal.ZERO
    val netRealizedGainAmount = account.netRealizedGain(investmentsInAccount)
    val buyTransactions = investmentsInAccount.flatMap { it.transactions }
        .filter { it.type == InvestmentTransactionType.BUY }
    val totalContribution = buyTransactions.fold(BigDecimal.ZERO) { acc, t -> acc + t.netAmount }
    val contributionThisYear = buyTransactions
        .filter { it.date.year == currentYear }
        .fold(BigDecimal.ZERO) { acc, t -> acc + t.netAmount }

    val currentRows = listOf(
        stringResource(R.string.investments_label_current_account_balance) to account.balance,
        stringResource(R.string.investments_label_net_deposits) to netDeposits,
        stringResource(R.string.investments_label_total_contribution) to totalContribution,
        stringResource(R.string.investments_label_cost_basis) to account.investmentCostBasis,
        stringResource(R.string.investments_label_uninvested_cash) to account.uninvestedCash,
        stringResource(R.string.investments_label_unrealized_gain) to unrealizedGainAmount,
        stringResource(R.string.investments_label_current_net) to netUnrealizedGainAmount
    )
    val thisYearRows = listOf(
        stringResource(R.string.investments_label_contribution) to contributionThisYear,
        stringResource(R.string.investments_label_realized_gain) to realizedGainThisYear,
        stringResource(R.string.investments_label_net_realized_gain) to netRealizedGainThisYear
    )
    val totalRows = listOf(
        stringResource(R.string.investments_label_realized_gain) to realizedGainAmount,
        stringResource(R.string.investments_label_net_realized_gain) to netRealizedGainAmount
    )
    val currentTaxLabel = stringResource(R.string.investments_label_current_tax, account.taxRate.displayName())
    val totalTaxLabel = stringResource(R.string.investments_label_total_tax, account.taxRate.displayName())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item { DetailSectionHeader(stringResource(R.string.investments_section_current)) }
        itemsIndexed(currentRows, key = { _, (label, _) -> label }) { index, (label, value) ->
            if (index > 0) HorizontalDivider()
            DetailRow(label = label, value = value, currency = currency)
        }

        item { DetailSectionHeader(stringResource(R.string.investments_section_this_year)) }
        itemsIndexed(thisYearRows, key = { _, (label, _) -> "thisyear_$label" }) { index, (label, value) ->
            if (index > 0) HorizontalDivider()
            DetailRow(label = label, value = value, currency = currency)
        }
        if (realizedTaxThisYear.signum() > 0) {
            item { HorizontalDivider() }
            item {
                DetailRow(
                    label = currentTaxLabel,
                    value = realizedTaxThisYear,
                    currency = currency,
                    negative = true
                )
            }
        }

        item { DetailSectionHeader(stringResource(R.string.investments_section_total)) }
        itemsIndexed(totalRows, key = { _, (label, _) -> "total_$label" }) { index, (label, value) ->
            if (index > 0) HorizontalDivider()
            DetailRow(label = label, value = value, currency = currency)
        }
        if (realizedTax.signum() > 0) {
            item { HorizontalDivider() }
            item {
                DetailRow(
                    label = totalTaxLabel,
                    value = realizedTax,
                    currency = currency,
                    negative = true
                )
            }
        }
    }
}

@Composable
private fun DetailSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
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
