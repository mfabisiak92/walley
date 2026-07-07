package com.walley.app.feature.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.PieChartCard
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.PieSlice
import com.walley.app.domain.model.Budget
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetWizardScreen(
    onDone: (Long) -> Unit,
    onCancel: () -> Unit,
    viewModel: BudgetWizardViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val step = viewModel.currentStep
    val section = viewModel.sectionForStep(step)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section?.label ?: if (step == WIZARD_STEP_SUMMARY) "Summary" else "New budget") },
                navigationIcon = {
                    IconButton(onClick = { if (step == WIZARD_STEP_MONTH) onCancel() else viewModel.goBack() }) {
                        Icon(
                            if (step == WIZARD_STEP_MONTH) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        when (step) {
                            WIZARD_STEP_SUMMARY -> scope.launch { onDone(viewModel.createBudget()) }
                            else -> viewModel.goNext()
                        }
                    },
                    enabled = step != WIZARD_STEP_MONTH || viewModel.monthTaken != 1
                ) {
                    Text(if (step == WIZARD_STEP_SUMMARY) "Create budget" else "Next")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                step == WIZARD_STEP_MONTH -> MonthStep(viewModel)
                step == WIZARD_STEP_SUMMARY -> SummaryStep(viewModel)
                section != null -> SectionStep(viewModel, section)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthStep(viewModel: BudgetWizardViewModel) {
    LaunchedEffect(viewModel.year, viewModel.month) {
        viewModel.refreshMonthTaken()
    }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var yearMenuExpanded by remember { mutableStateOf(false) }
    val currentYear = java.time.LocalDate.now().year

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Choose the month for this budget", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExposedDropdownMenuBox(
                expanded = monthMenuExpanded,
                onExpandedChange = { monthMenuExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = Month.of(viewModel.month).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Month") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthMenuExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false }
                ) {
                    (1..12).forEach { m ->
                        DropdownMenuItem(
                            text = { Text(Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH)) },
                            onClick = {
                                viewModel.setYearMonth(viewModel.year, m)
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = yearMenuExpanded,
                onExpandedChange = { yearMenuExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = viewModel.year.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Year") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearMenuExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = yearMenuExpanded,
                    onDismissRequest = { yearMenuExpanded = false }
                ) {
                    (currentYear - 1..currentYear + 5).forEach { y ->
                        DropdownMenuItem(
                            text = { Text(y.toString()) },
                            onClick = {
                                viewModel.setYearMonth(y, viewModel.month)
                                yearMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        if (viewModel.monthTaken == 1) {
            Text(
                "A budget for ${Budget(year = viewModel.year, month = viewModel.month).displayName} already exists.",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SectionStep(viewModel: BudgetWizardViewModel, section: BudgetSectionType) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDraft by remember { mutableStateOf<WizardItemDraft?>(null) }
    val items = viewModel.itemsFor(section)
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val isAccountLinked = section == BudgetSectionType.SAVINGS || section == BudgetSectionType.INVESTMENTS
    val requiresCashAccount = section == BudgetSectionType.INCOME ||
        section == BudgetSectionType.INCOME_RELATED_EXPENSES
    val showFooter = section != BudgetSectionType.INCOME && section != BudgetSectionType.INCOME_RELATED_EXPENSES
    val linkedAccounts = if (isAccountLinked) viewModel.accountsFor(section) else emptyList()
    val cashAccounts = if (requiresCashAccount) viewModel.accountsFor(section) else emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            if ((isAccountLinked && linkedAccounts.isEmpty()) || (requiresCashAccount && cashAccounts.isEmpty())) {
                val kind = when (section) {
                    BudgetSectionType.SAVINGS -> "saving"
                    BudgetSectionType.INVESTMENTS -> "investment"
                    else -> "cash or checking"
                }
                Text(
                    "No $kind accounts yet — create one from the Accounts tab, or skip this section.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(items, key = { it.localId }) { draft ->
                    WizardItemRow(
                        draft = draft,
                        accountName = accounts.find { it.id == draft.accountId }?.name,
                        onClick = { editingDraft = draft },
                        onRemove = { viewModel.removeItem(section, draft.localId) }
                    )
                }
            }
            Button(
                onClick = { showAddDialog = true },
                enabled = (!isAccountLinked || linkedAccounts.isNotEmpty()) &&
                    (!requiresCashAccount || cashAccounts.isNotEmpty()),
                modifier = Modifier.padding(top = 8.dp)
            ) { Text("Add item") }
        }
        if (showFooter) {
            SectionFooter(viewModel, section)
        }
    }

    if (showAddDialog || editingDraft != null) {
        val initial = editingDraft
        if (isAccountLinked) {
            AddAccountLinkedItemDialog(
                accounts = linkedAccounts,
                initial = initial,
                onDismiss = {
                    showAddDialog = false
                    editingDraft = null
                },
                onConfirm = { accountId, amount, day, lastOfMonth ->
                    val account = linkedAccounts.find { it.id == accountId }
                    if (account != null) {
                        val draft = WizardItemDraft(
                            localId = initial?.localId ?: System.nanoTime(),
                            name = account.name,
                            amount = amount,
                            currency = account.currency,
                            accountId = accountId,
                            paymentDay = day,
                            paymentDayIsLastOfMonth = lastOfMonth
                        )
                        if (initial != null) {
                            viewModel.updateItem(section, initial.localId, draft)
                        } else {
                            viewModel.addItem(section, draft)
                        }
                    }
                    showAddDialog = false
                    editingDraft = null
                }
            )
        } else {
            AddBudgetItemDialog(
                currency = baseCurrency,
                initial = initial,
                accounts = cashAccounts,
                requireAccount = requiresCashAccount,
                onDismiss = {
                    showAddDialog = false
                    editingDraft = null
                },
                onConfirm = { name, amount, day, lastOfMonth, accountId ->
                    val draft = WizardItemDraft(
                        localId = initial?.localId ?: System.nanoTime(),
                        name = name,
                        amount = amount,
                        currency = baseCurrency,
                        accountId = accountId,
                        paymentDay = day,
                        paymentDayIsLastOfMonth = lastOfMonth
                    )
                    if (initial != null) {
                        viewModel.updateItem(section, initial.localId, draft)
                    } else {
                        viewModel.addItem(section, draft)
                    }
                    showAddDialog = false
                    editingDraft = null
                }
            )
        }
    }
}

@Composable
private fun WizardItemRow(draft: WizardItemDraft, accountName: String?, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(draft.name, style = MaterialTheme.typography.bodyLarge)
            val dayLabel = when {
                draft.paymentDayIsLastOfMonth -> "Last day of month"
                draft.paymentDay != null -> "Day ${draft.paymentDay}"
                else -> null
            }
            if (dayLabel != null) {
                Text(dayLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row {
            Text(formatMoney(draft.amount, draft.currency), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${draft.name}")
            }
        }
    }
}

@Composable
private fun SectionFooter(viewModel: BudgetWizardViewModel, section: BudgetSectionType) {
    // Collected (not just read via .value) so this composable recomposes once rates load.
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val disposable = viewModel.disposableIncome
    val sectionTotal = viewModel.sectionTotal(section)
    val unallocated = viewModel.unallocatedAmount()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        HorizontalDivider()
        Column(modifier = Modifier.padding(top = 8.dp)) {
            if (unallocated == null || sectionTotal == null) {
                Text(
                    "Exchange rate unavailable — allocation can't be computed for this section right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val percent = if (disposable.signum() == 0) {
                    BigDecimal.ZERO
                } else {
                    sectionTotal.divide(disposable, 4, RoundingMode.HALF_UP) * BigDecimal(100)
                }
                Text(
                    "Unallocated: ${formatMoney(unallocated, baseCurrency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${percent.setScale(1, RoundingMode.HALF_UP)}% of disposable income allocated to ${section.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryStep(viewModel: BudgetWizardViewModel) {
    // Collected (not just read via .value) so this composable recomposes once rates load.
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val fixed = viewModel.sectionTotal(BudgetSectionType.FIXED_COSTS)
    val other = viewModel.sectionTotal(BudgetSectionType.OTHER_COSTS)
    val savings = viewModel.sectionTotal(BudgetSectionType.SAVINGS)
    val investments = viewModel.sectionTotal(BudgetSectionType.INVESTMENTS)
    val unallocated = viewModel.unallocatedAmount()
    val disposable = viewModel.disposableIncome

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            Budget(year = viewModel.year, month = viewModel.month).displayName,
            style = MaterialTheme.typography.titleLarge
        )
        SummaryRow("Income", viewModel.totalIncome, baseCurrency)
        SummaryRow("Income-related expenses", viewModel.totalIncomeExpenses, baseCurrency)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SummaryRow("Disposable income", disposable, baseCurrency)
        SummaryRow("Fixed costs", fixed, baseCurrency)
        SummaryRow("Other costs", other, baseCurrency)
        SummaryRow("Savings", savings, baseCurrency)
        SummaryRow("Investments", investments, baseCurrency)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        SummaryRow("Unallocated", unallocated, baseCurrency)

        if (disposable.signum() > 0 && fixed != null && other != null && savings != null && investments != null) {
            val labels = listOf("Fixed costs", "Other costs", "Savings", "Investments")
            val amounts = listOf(fixed, other, savings, investments)
            val slices = labels.indices.mapNotNull { index ->
                val amount = amounts[index]
                if (amount.signum() <= 0) return@mapNotNull null
                val percent = amount.divide(disposable, 4, RoundingMode.HALF_UP) * BigDecimal(100)
                PieSlice(
                    label = "${labels[index]} · ${percent.setScale(1, RoundingMode.HALF_UP)}% · " +
                        formatMoney(amount, baseCurrency),
                    percent = percent.toFloat(),
                    color = PieChartColors[index % PieChartColors.size]
                )
            }
            if (slices.isNotEmpty()) {
                PieChartCard(title = "Budget allocation", slices = slices)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: BigDecimal?, currency: Currency) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            amount?.let { formatMoney(it, currency) } ?: "—",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
