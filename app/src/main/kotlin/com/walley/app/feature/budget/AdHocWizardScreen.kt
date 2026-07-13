package com.walley.app.feature.budget

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.BudgetItemIconBadge
import com.walley.app.domain.model.Currency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdHocWizardScreen(
    onDone: (Long) -> Unit,
    onCancel: () -> Unit,
    viewModel: AdHocWizardViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val step = viewModel.currentStep
    var showExceedsBalanceConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (step == AD_HOC_STEP_DETAILS) onCancel() else viewModel.goBack()
    }

    val title = when (step) {
        AD_HOC_STEP_DETAILS -> "New ad-hoc budget"
        AD_HOC_STEP_ITEMS -> "Expenses"
        else -> "Summary"
    }
    val nextEnabled = when (step) {
        AD_HOC_STEP_DETAILS -> viewModel.detailsValid
        else -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { if (step == AD_HOC_STEP_DETAILS) onCancel() else viewModel.goBack() }) {
                        Icon(
                            if (step == AD_HOC_STEP_DETAILS) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (step != AD_HOC_STEP_DETAILS) {
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
                            AD_HOC_STEP_SUMMARY -> if (viewModel.exceedsAccountBalance) {
                                showExceedsBalanceConfirm = true
                            } else {
                                scope.launch { onDone(viewModel.createBudget()) }
                            }
                            else -> viewModel.goNext()
                        }
                    },
                    enabled = nextEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (step == AD_HOC_STEP_SUMMARY) "Create budget" else "Next")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (step) {
                AD_HOC_STEP_DETAILS -> DetailsStep(viewModel)
                AD_HOC_STEP_ITEMS -> ItemsStep(viewModel)
                else -> AdHocSummaryStep(viewModel)
            }
        }
    }

    if (showExceedsBalanceConfirm) {
        AlertDialog(
            onDismissRequest = { showExceedsBalanceConfirm = false },
            title = { Text("Create budget anyway?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This budget's total exceeds what's currently available in one or more accounts:")
                    viewModel.totalsByAccount.forEach { (account, planned) ->
                        if (planned > account.balance) {
                            Text(
                                "${account.name}: needs ${formatMoney(planned, account.currency)}, " +
                                    "has ${formatMoney(account.balance, account.currency)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExceedsBalanceConfirm = false
                        scope.launch { onDone(viewModel.createBudget()) }
                    }
                ) { Text("Create anyway") }
            },
            dismissButton = {
                TextButton(onClick = { showExceedsBalanceConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsStep(viewModel: AdHocWizardViewModel) {
    val savingAccounts by viewModel.savingAccounts.collectAsStateWithLifecycle()
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val selectedAccount = viewModel.selectedAccount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = viewModel.name,
            onValueChange = { viewModel.name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Start date")
            TextButton(onClick = { showStartDatePicker = true }) {
                Text(viewModel.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("End date")
            TextButton(onClick = { showEndDatePicker = true }) {
                Text(viewModel.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
            }
        }
        if (viewModel.endDate.isBefore(viewModel.startDate)) {
            Text(
                "End date can't be before the start date.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (savingAccounts.isEmpty()) {
            Text(
                "No saving accounts yet — create one from the Accounts tab first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = accountMenuExpanded,
                onExpandedChange = { accountMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Draw money from") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false }
                ) {
                    savingAccounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                viewModel.selectAccount(account.id)
                                accountMenuExpanded = false
                            }
                        )
                    }
                }
            }
            selectedAccount?.let { account ->
                Text(
                    "Currently: ${formatMoney(account.balance, account.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.startDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.startDate = millisToLocalDate(millis)
                        }
                        showStartDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.endDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.endDate = millisToLocalDate(millis)
                        }
                        showEndDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun ItemsStep(viewModel: AdHocWizardViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDraft by remember { mutableStateOf<AdHocItemDraft?>(null) }
    val savingAccounts by viewModel.savingAccounts.collectAsStateWithLifecycle()
    val account = viewModel.selectedAccount

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.items, key = { it.localId }) { draft ->
                    val itemAccount = viewModel.accountFor(draft)
                    AdHocWizardItemRow(
                        draft = draft,
                        currency = itemAccount?.currency,
                        accountName = itemAccount?.takeIf { it.id != account?.id }?.name,
                        onClick = { editingDraft = draft },
                        onRemove = { viewModel.removeItem(draft.localId) }
                    )
                }
            }
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Add item", modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (account != null) {
            HorizontalDivider()
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                viewModel.totalsByAccount.forEach { (itemAccount, planned) ->
                    Text(
                        "Total in ${itemAccount.name}: ${formatMoney(planned, itemAccount.currency)} · " +
                            "has ${formatMoney(itemAccount.balance, itemAccount.currency)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (viewModel.exceedsAccountBalance) {
                    Text(
                        "This is more than what's currently available in one or more accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if ((showAddDialog || editingDraft != null) && account != null) {
        val initial = editingDraft
        AddAdHocItemDialog(
            defaultAccount = account,
            accounts = savingAccounts,
            initial = initial,
            onDismiss = {
                showAddDialog = false
                editingDraft = null
            },
            onConfirm = { name, amount, icon, itemAccountId ->
                val draft = AdHocItemDraft(
                    localId = initial?.localId ?: System.nanoTime(),
                    name = name,
                    amount = amount,
                    icon = icon,
                    accountId = itemAccountId
                )
                if (initial != null) {
                    viewModel.updateItem(initial.localId, draft)
                } else {
                    viewModel.addItem(draft)
                }
                showAddDialog = false
                editingDraft = null
            }
        )
    }
}

@Composable
private fun AdHocWizardItemRow(
    draft: AdHocItemDraft,
    currency: Currency?,
    accountName: String?,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            BudgetItemIconBadge(icon = draft.icon)
            Column {
                Text(draft.name, style = MaterialTheme.typography.bodyLarge)
                if (accountName != null) {
                    Text(
                        "From $accountName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currency != null) {
                Text(formatMoney(draft.amount, currency), style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove ${draft.name}")
            }
        }
    }
}

@Composable
private fun AdHocSummaryStep(viewModel: AdHocWizardViewModel) {
    val account = viewModel.selectedAccount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(viewModel.name, style = MaterialTheme.typography.titleLarge)
        Text(
            "${viewModel.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} – " +
                viewModel.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (account != null) {
            Text(
                "Default account: ${account.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            viewModel.items.forEach { draft ->
                val itemAccount = viewModel.accountFor(draft) ?: account
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(draft.name, style = MaterialTheme.typography.bodyLarge)
                        if (itemAccount.id != account.id) {
                            Text(
                                "From ${itemAccount.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(formatMoney(draft.amount, itemAccount.currency), style = MaterialTheme.typography.bodyLarge)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            viewModel.totalsByAccount.forEach { (itemAccount, planned) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (viewModel.totalsByAccount.size > 1) "Total (${itemAccount.name})" else "Total",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(formatMoney(planned, itemAccount.currency), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Currently in ${itemAccount.name}: ${formatMoney(itemAccount.balance, itemAccount.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (viewModel.exceedsAccountBalance) {
                Text(
                    "This budget's total exceeds what's currently available in one or more accounts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
