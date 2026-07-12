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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Currency
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
                title = { Text("Net worth breakdown") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    Text("Total", style = MaterialTheme.typography.titleMedium)
                    if (state.amount != null) {
                        Text(formatMoney(state.amount, state.currency), style = MaterialTheme.typography.headlineMedium)
                        state.rateDate?.let { date ->
                            Text(
                                "ECB rates · $date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text("Exchange rates unavailable", style = MaterialTheme.typography.bodyLarge)
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
        }
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
            Text(category.label, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatMoney(total, baseCurrency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (total.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
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
