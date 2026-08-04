package com.walley.app.feature.budget

import com.walley.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.domain.model.AccountEffectsGroup
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetSettingsViewModel = hiltViewModel()
) {
    val budgetWithItems by viewModel.budget.collectAsStateWithLifecycle()
    val budget = budgetWithItems?.budget
    val isEditable = budgetWithItems?.budget?.status != BudgetStatus.COMPLETED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budget_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.budget_draw_from_linked_accounts_title), style = MaterialTheme.typography.titleMedium)
            Text(
                "When off for a category, paying its items won't move money in any account. " +
                    "Only affects future actions — balance changes already applied are left as-is.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            if (budget != null) {
                AccountEffectsToggleRow(
                    label = AccountEffectsGroup.INCOME.displayName(),
                    checked = budget.applyIncomeAccountEffects,
                    onCheckedChange = viewModel::updateIncomeAccountEffects,
                    enabled = isEditable
                )
                AccountEffectsToggleRow(
                    label = AccountEffectsGroup.COSTS.displayName(),
                    checked = budget.applyCostsAccountEffects,
                    onCheckedChange = viewModel::updateCostsAccountEffects,
                    enabled = isEditable
                )
                AccountEffectsToggleRow(
                    label = AccountEffectsGroup.SAVINGS.displayName(),
                    checked = budget.applySavingsAccountEffects,
                    onCheckedChange = viewModel::updateSavingsAccountEffects,
                    enabled = isEditable
                )
                AccountEffectsToggleRow(
                    label = AccountEffectsGroup.INVESTMENTS.displayName(),
                    checked = budget.applyInvestmentsAccountEffects,
                    onCheckedChange = viewModel::updateInvestmentsAccountEffects,
                    enabled = isEditable
                )
            }
        }
    }
}
