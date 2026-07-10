package com.walley.app.feature.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.WalleyTopBar
import com.walley.app.domain.model.AdHocBudgetWithItems
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val TABS = listOf("Monthly", "Ad-hoc")

@Composable
fun BudgetsScreen(
    modifier: Modifier = Modifier,
    onNavigateHome: () -> Unit,
    onCreateBudget: () -> Unit,
    onOpenBudget: (Long) -> Unit,
    onResumeDraft: (Long) -> Unit,
    onCreateAdHocBudget: () -> Unit,
    onResumeAdHocDraft: (Long) -> Unit,
    onOpenAdHocBudget: (Long) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = { WalleyTopBar(onTitleClick = onNavigateHome) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize()
        ) {
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
                when (page) {
                    0 -> MonthlyBudgetsPage(
                        onCreateBudget = onCreateBudget,
                        onOpenBudget = onOpenBudget,
                        onResumeDraft = onResumeDraft
                    )
                    else -> AdHocBudgetsPage(
                        onCreateBudget = onCreateAdHocBudget,
                        onOpenBudget = onOpenAdHocBudget,
                        onResumeDraft = onResumeAdHocDraft
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyBudgetsPage(
    onCreateBudget: () -> Unit,
    onOpenBudget: (Long) -> Unit,
    onResumeDraft: (Long) -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()

    Scaffold(
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
                items(budgets, key = { it.budgetWithItems.budget.id }) { row ->
                    val isDraft = row.budgetWithItems.budget.status == BudgetStatus.DRAFT
                    BudgetRow(
                        row = row,
                        onClick = {
                            if (isDraft) {
                                onResumeDraft(row.budgetWithItems.budget.id)
                            } else {
                                onOpenBudget(row.budgetWithItems.budget.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetRow(row: BudgetRowData, onClick: () -> Unit) {
    val status = row.budgetWithItems.budget.status
    val isCompleted = status == BudgetStatus.COMPLETED
    val isDraft = status == BudgetStatus.DRAFT
    val isMuted = isCompleted || isDraft

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isMuted) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(row.budgetWithItems.budget.displayName, style = MaterialTheme.typography.titleMedium)
                if (isCompleted) {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isDraft) {
                    Text(
                        "Draft",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isDraft) {
                Text(
                    "Tap to continue setting up this budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Disposable income: " + (row.disposableIncome?.let { formatMoney(it, row.baseCurrency) } ?: "—"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Unallocated: " + (row.unallocated?.let { formatMoney(it, row.baseCurrency) }
                        ?: "exchange rate unavailable"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.progress == null) {
                    Text(
                        "Progress unavailable — exchange rate missing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${formatMoney(row.progress.spent, row.baseCurrency)} / " +
                                formatMoney(row.progress.planned, row.baseCurrency),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${row.progress.percent.setScale(0, RoundingMode.HALF_UP)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = {
                            row.progress.percent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
                                .toFloat()
                                .coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun AdHocBudgetsPage(
    onCreateBudget: () -> Unit,
    onOpenBudget: (Long) -> Unit,
    onResumeDraft: (Long) -> Unit,
    viewModel: AdHocBudgetsViewModel = hiltViewModel()
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateBudget) {
                Icon(Icons.Default.Add, contentDescription = "Create ad-hoc budget")
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
                    "No ad-hoc budgets yet — tap + to create one.",
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
                items(budgets, key = { it.budget.id }) { row ->
                    AdHocBudgetRow(
                        row = row,
                        currencyFor = viewModel::currencyFor,
                        onClick = {
                            if (row.budget.isDraft) {
                                onResumeDraft(row.budget.id)
                            } else {
                                onOpenBudget(row.budget.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdHocBudgetRow(
    row: AdHocBudgetWithItems,
    currencyFor: (Long) -> Currency?,
    onClick: () -> Unit
) {
    val isDraft = row.budget.isDraft
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isDraft) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(row.budget.name, style = MaterialTheme.typography.titleMedium)
                if (isDraft) {
                    Text(
                        "Draft",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${row.budget.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} – " +
                    row.budget.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isDraft) {
                Text(
                    "Tap to continue setting up this budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                row.totalsByCurrency(currencyFor).forEach { total ->
                    val percent = if (total.planned.signum() == 0) {
                        BigDecimal.ZERO
                    } else {
                        (total.paid.divide(total.planned, 6, RoundingMode.HALF_UP) * BigDecimal(100))
                            .coerceIn(BigDecimal.ZERO, BigDecimal(100))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${formatMoney(total.paid, total.currency)} / ${formatMoney(total.planned, total.currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${percent.setScale(0, RoundingMode.HALF_UP)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = {
                            percent.divide(BigDecimal(100), 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
