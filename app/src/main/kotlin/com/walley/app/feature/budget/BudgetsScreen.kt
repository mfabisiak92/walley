package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    onCreateBudget: () -> Unit,
    onOpenBudget: (Long) -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { WalleyTopBar(onTitleClick = onNavigateHome) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateBudget) {
                Icon(Icons.Default.Add, contentDescription = "Create budget")
            }
        }
    ) { innerPadding ->
        if (budgets.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Calculate,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "No budgets yet — tap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(budgets, key = { it.budget.id }) { budgetWithItems ->
                    BudgetRow(budgetWithItems = budgetWithItems, onClick = { onOpenBudget(budgetWithItems.budget.id) })
                }
            }
        }
    }
}

@Composable
private fun BudgetRow(budgetWithItems: BudgetWithItems, onClick: () -> Unit) {
    val income = budgetWithItems.items.filter { it.section == BudgetSectionType.INCOME }.sumOf { it.amount }
    val expenses = budgetWithItems.items
        .filter { it.section == BudgetSectionType.INCOME_RELATED_EXPENSES }
        .sumOf { it.amount }
    val disposable = income - expenses

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(budgetWithItems.budget.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                "Disposable income: ${formatMoney(disposable, Currency.PLN)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
