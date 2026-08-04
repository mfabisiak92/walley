package com.walley.app.feature.budget

import com.walley.app.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.BudgetItemIconBadge
import com.walley.app.core.ui.PieChartCard
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.PieSlice
import com.walley.app.domain.model.AccountEffectsGroup
import com.walley.app.domain.model.Budget
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.displayName
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

    BackHandler {
        if (step == WIZARD_STEP_MONTH) onCancel() else viewModel.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section?.displayName() ?: if (step == WIZARD_STEP_SUMMARY) "Summary" else "New budget") },
                navigationIcon = {
                    IconButton(onClick = { if (step == WIZARD_STEP_MONTH) onCancel() else viewModel.goBack() }) {
                        Icon(
                            if (step == WIZARD_STEP_MONTH) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Progress is autosaved as a Draft on every change, so it's always safe to jump
                    // straight out from any step rather than making the user go back one at a time.
                    if (step != WIZARD_STEP_MONTH) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        when (step) {
                            WIZARD_STEP_SUMMARY -> scope.launch { onDone(viewModel.createBudget()) }
                            else -> viewModel.goNext()
                        }
                    },
                    enabled = step != WIZARD_STEP_MONTH || viewModel.monthTaken != 1,
                    modifier = Modifier.fillMaxWidth()
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.budget_choose_month_title), style = MaterialTheme.typography.titleMedium)
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
                    label = { Text(stringResource(R.string.budget_month_label)) },
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
                    label = { Text(stringResource(R.string.budget_year_label)) },
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
        HorizontalDivider()
        Text(stringResource(R.string.budget_draw_from_linked_accounts_title), style = MaterialTheme.typography.bodyLarge)
        Text(
            "When off for a category, paying its items won't move money in any account. " +
                "This can be changed later from the budget's settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AccountEffectsToggleRow(
            label = AccountEffectsGroup.INCOME.displayName(),
            checked = viewModel.applyIncomeAccountEffects,
            onCheckedChange = { viewModel.applyIncomeAccountEffects = it }
        )
        AccountEffectsToggleRow(
            label = AccountEffectsGroup.COSTS.displayName(),
            checked = viewModel.applyCostsAccountEffects,
            onCheckedChange = { viewModel.applyCostsAccountEffects = it }
        )
        AccountEffectsToggleRow(
            label = AccountEffectsGroup.SAVINGS.displayName(),
            checked = viewModel.applySavingsAccountEffects,
            onCheckedChange = { viewModel.applySavingsAccountEffects = it }
        )
        AccountEffectsToggleRow(
            label = AccountEffectsGroup.INVESTMENTS.displayName(),
            checked = viewModel.applyInvestmentsAccountEffects,
            onCheckedChange = { viewModel.applyInvestmentsAccountEffects = it }
        )
    }
}

/** Same mapping [BudgetWizardViewModel.collectItems] falls back to at save time — applied here too so a freshly added item shows its icon immediately in the wizard's own list. */
internal fun defaultIconFor(section: BudgetSectionType): BudgetItemIcon? = when (section) {
    BudgetSectionType.SAVINGS -> BudgetItemIcon.SAVING
    BudgetSectionType.INVESTMENTS -> BudgetItemIcon.INVESTMENT
    else -> null
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
    val allowsOptionalAccount = section == BudgetSectionType.OTHER_COSTS || section == BudgetSectionType.FIXED_COSTS
    val showFooter = section != BudgetSectionType.INCOME && section != BudgetSectionType.INCOME_RELATED_EXPENSES
    val linkedAccounts = if (isAccountLinked) viewModel.accountsFor(section) else emptyList()
    val cashAccounts = if (requiresCashAccount || allowsOptionalAccount) viewModel.accountsFor(section) else emptyList()

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
                    else -> "cash, checking, or investment"
                }
                Text(
                    "No $kind accounts yet — create one from the Accounts tab, or skip this section.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items, key = { it.localId }) { draft ->
                    WizardItemRow(
                        draft = draft,
                        accountName = accounts.find { it.id == draft.accountId }?.name,
                        isAccountLinked = isAccountLinked,
                        onClick = { editingDraft = draft },
                        onRemove = { viewModel.removeItem(section, draft.localId) }
                    )
                }
            }
            OutlinedButton(
                onClick = { showAddDialog = true },
                enabled = (!isAccountLinked || linkedAccounts.isNotEmpty()) &&
                    (!requiresCashAccount || cashAccounts.isNotEmpty()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.budget_add_item), modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (showFooter) {
            SectionFooter(viewModel, section)
        }
    }

    if (showAddDialog || editingDraft != null) {
        val initial = editingDraft
        if (isAccountLinked) {
            // Other drafts in this section already claim these accounts — exclude them so the same
            // account can't be double-booked, but keep the draft being edited's own account selectable.
            val excludedAccountIds = if (section == BudgetSectionType.SAVINGS) {
                items.filter { it.localId != initial?.localId }.mapNotNull { it.accountId }.toSet()
            } else {
                emptySet()
            }
            AddAccountLinkedItemDialog(
                accounts = linkedAccounts,
                initial = initial,
                excludedAccountIds = excludedAccountIds,
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
                            paymentDayIsLastOfMonth = lastOfMonth,
                            // Set immediately so the wizard's own list shows it right away, rather than
                            // relying on collectItems()'s same fallback, which only applies at save time.
                            icon = initial?.icon ?: defaultIconFor(section)
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
                showAccountPicker = requiresCashAccount || allowsOptionalAccount,
                showCategoryPicker = section == BudgetSectionType.INCOME,
                onDismiss = {
                    showAddDialog = false
                    editingDraft = null
                },
                onConfirm = { name, amount, day, lastOfMonth, accountId, incomeCategory, icon ->
                    val draft = WizardItemDraft(
                        localId = initial?.localId ?: System.nanoTime(),
                        name = name,
                        amount = amount,
                        currency = cashAccounts.find { it.id == accountId }?.currency ?: baseCurrency,
                        accountId = accountId,
                        paymentDay = day,
                        paymentDayIsLastOfMonth = lastOfMonth,
                        incomeCategory = incomeCategory,
                        icon = icon
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
private fun WizardItemRow(
    draft: WizardItemDraft,
    accountName: String?,
    isAccountLinked: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    // Mirrors BudgetItemRow's treatment: a SAVINGS/INVESTMENTS draft's title should track the
    // account it's linked to, not the name copied from it when the item was first added.
    val displayName = if (isAccountLinked) accountName ?: draft.name else draft.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BudgetItemIconBadge(icon = draft.icon)
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(displayName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val dayLabel = when {
                    draft.paymentDayIsLastOfMonth -> "Last day of month"
                    draft.paymentDay != null -> "Day ${draft.paymentDay}"
                    else -> null
                }
                if (dayLabel != null) {
                    Text(dayLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatMoney(draft.amount, draft.currency), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove $displayName")
            }
        }
    }
}

@Composable
private fun SectionFooter(viewModel: BudgetWizardViewModel, section: BudgetSectionType) {
    // Collected (not just read via .value) so this composable recomposes once rates load.
    val baseCurrency by viewModel.baseCurrency.collectAsStateWithLifecycle()
    val categoryTargets by viewModel.categoryTargets.collectAsStateWithLifecycle()
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
                    "Allocated to ${section.displayName()}: ${formatMoney(sectionTotal, baseCurrency)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${percent.setScale(1, RoundingMode.HALF_UP)}% of disposable income · " +
                        "Unallocated overall: ${formatMoney(unallocated, baseCurrency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CategoryTargetIndicator(
                    section = section,
                    actualPercent = percent,
                    targetPercent = categoryTargets[section],
                    modifier = Modifier.padding(top = 8.dp)
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
                    label = labels[index],
                    value = formatMoney(amount, baseCurrency),
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
