package com.walley.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.R
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.displayName
import com.walley.app.feature.budget.ProjectedNetWorthBreakdown
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val netWorth by viewModel.netWorth.collectAsStateWithLifecycle()
    var expandedCategories by remember { mutableStateOf(setOf<NetWorthCategory>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_net_worth_breakdown)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.home_back_content_description))
                    }
                }
            )
        }
    ) { innerPadding ->
        val state = netWorth
        if (state == null) {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {}
            return@Scaffold
        }

        val groups = NetWorthCategory.entries.mapNotNull { category ->
            val elements = state.elements.filter { it.category == category }
            if (elements.isEmpty()) null else category to elements
        }

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_total), style = MaterialTheme.typography.titleMedium)
                    if (state.amount != null) {
                        Text(formatMoney(state.amount, state.currency), style = MaterialTheme.typography.headlineMedium)
                        state.rateDate?.let { date ->
                            Text(
                                stringResource(R.string.home_ecb_rates, date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(stringResource(R.string.home_exchange_rates_unavailable), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            groups.forEach { (category, elements) ->
                val expanded = category in expandedCategories
                item(key = "header_${category.name}") {
                    NetWorthCategoryHeaderRow(
                        category = category,
                        total = elements.fold(BigDecimal.ZERO) { acc, element -> acc + element.amountInBaseCurrency },
                        baseCurrency = state.currency,
                        expanded = expanded,
                        onToggle = {
                            expandedCategories = if (expanded) {
                                expandedCategories - category
                            } else {
                                expandedCategories + category
                            }
                        }
                    )
                }
                if (expanded) {
                    items(elements, key = { "elem_${category.name}_${it.name}_${it.currency.name}" }) { element ->
                        NetWorthElementRow(element = element, baseCurrency = state.currency)
                    }
                }
            }

            val projectedBreakdown = state.projectedBreakdown
            val projectedAmount = state.projectedAmount
            if (projectedBreakdown != null && projectedAmount != null && state.amount != null) {
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_projected_net_worth), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.home_projected_net_worth_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    ProjectedNetWorthCard(
                        currentNetWorth = state.amount,
                        breakdown = projectedBreakdown,
                        projectedAmount = projectedAmount,
                        plannedAmount = state.plannedNetWorth,
                        currency = state.currency
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectedNetWorthCard(
    currentNetWorth: BigDecimal,
    breakdown: ProjectedNetWorthBreakdown,
    projectedAmount: BigDecimal,
    plannedAmount: BigDecimal?,
    currency: Currency
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ProjectedNetWorthRow(stringResource(R.string.home_current_net_worth), currentNetWorth, currency)
            ProjectedNetWorthRow(stringResource(R.string.home_income), breakdown.income, currency, signed = true)
            ProjectedNetWorthRow(stringResource(R.string.home_income_related_expenses), -breakdown.incomeRelatedExpenses, currency, signed = true)
            ProjectedNetWorthRow(stringResource(R.string.home_fixed_costs), -breakdown.fixedCosts, currency, signed = true)
            ProjectedNetWorthRow(stringResource(R.string.home_other_costs), -breakdown.otherCosts, currency, signed = true)
            breakdown.savingsAdjustment?.let { adjustment ->
                ProjectedNetWorthRow(stringResource(R.string.home_savings_adjustment), adjustment, currency, signed = true)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ProjectedNetWorthRow(stringResource(R.string.home_projected_net_worth), projectedAmount, currency, bold = true)
            plannedAmount?.let { planned ->
                ProjectedNetWorthRow(stringResource(R.string.home_planned_net_worth), planned, currency)
            }
        }
    }
}

@Composable
private fun ProjectedNetWorthRow(
    label: String,
    value: BigDecimal,
    currency: Currency,
    signed: Boolean = false,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
        val sign = if (signed && value.signum() > 0) "+" else ""
        Text(
            sign + formatMoney(value, currency),
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = when {
                !signed -> MaterialTheme.colorScheme.onSurface
                value.signum() < 0 -> MaterialTheme.colorScheme.error
                value.signum() > 0 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun NetWorthCategoryHeaderRow(
    category: NetWorthCategory,
    total: BigDecimal,
    baseCurrency: Currency,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.displayName(), style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatMoney(total, baseCurrency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (total.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) {
                        stringResource(R.string.home_collapse_content_description)
                    } else {
                        stringResource(R.string.home_expand_content_description)
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NetWorthElementRow(element: NetWorthElement, baseCurrency: Currency) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(element.name, style = MaterialTheme.typography.bodyLarge)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMoney(element.amountInBaseCurrency, baseCurrency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (element.amountInBaseCurrency.signum() < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (element.currency != baseCurrency) {
                    Text(
                        formatMoney(element.originalAmount, element.currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
