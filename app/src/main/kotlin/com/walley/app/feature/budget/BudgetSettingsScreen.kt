package com.walley.app.feature.budget

import com.walley.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.format.toBigDecimalOrNullLenient
import com.walley.app.domain.model.AccountEffectsGroup
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.displayName
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetSettingsViewModel = hiltViewModel()
) {
    val budgetWithItems by viewModel.budget.collectAsStateWithLifecycle()
    val budget = budgetWithItems?.budget
    val isEditable = budgetWithItems?.budget?.status != BudgetStatus.COMPLETED
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    var showEditPlannedNetWorth by remember { mutableStateOf(false) }

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
                HorizontalDivider()
                Text(
                    "Planned net worth",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Your net-worth goal for this budget's month — shown next to \"Projected net worth\" on the Summary tab. Editable any time, even after the budget is completed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        budget.plannedNetWorth?.let { formatMoney(it, baseCurrency) } ?: "—",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(onClick = { showEditPlannedNetWorth = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit planned net worth")
                    }
                }
            }
        }
    }

    if (showEditPlannedNetWorth) {
        EditPlannedNetWorthDialog(
            initialValue = budget?.plannedNetWorth,
            currency = baseCurrency,
            onDismiss = { showEditPlannedNetWorth = false },
            onSave = { value ->
                viewModel.updatePlannedNetWorth(value)
                showEditPlannedNetWorth = false
            }
        )
    }
}

@Composable
internal fun EditPlannedNetWorthDialog(
    initialValue: BigDecimal?,
    currency: Currency,
    onDismiss: () -> Unit,
    onSave: (BigDecimal) -> Unit
) {
    var text by remember { mutableStateOf(initialValue?.toPlainString().orEmpty()) }
    val parsed = text.toBigDecimalOrNullLenient()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Planned net worth") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Amount ($currency)") },
                isError = text.isNotBlank() && parsed == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onSave) }, enabled = parsed != null) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
