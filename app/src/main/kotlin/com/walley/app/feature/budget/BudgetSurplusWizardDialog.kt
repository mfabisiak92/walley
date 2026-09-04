package com.walley.app.feature.budget

import com.walley.app.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.walley.app.core.format.formatMoney
import com.walley.app.core.format.toBigDecimalOrNullLenient
import com.walley.app.core.ui.InvestmentGainColor
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal

private class AllocationRowState(val currency: Currency) {
    var toAccountId by mutableStateOf<Long?>(null)
    var fromAccountId by mutableStateOf<Long?>(null)
    var amountText by mutableStateOf("")
}

/**
 * Shown when completing a budget that has leftover (underspent) money on Fixed/Other cost items —
 * lets the user split it across one or more Saving accounts, or skip and leave it unallocated. Money
 * moved into a non-virtual account is a real transfer from a chosen "from" account; a virtual
 * destination is credited directly (see [BudgetRepository.allocateSurplusToSavings]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSurplusWizardDialog(
    surplus: List<CurrencySurplus>,
    accounts: List<Account>,
    onSkip: () -> Unit,
    onComplete: (List<BudgetDetailViewModel.SurplusAllocationDraft>) -> Unit
) {
    val rows = remember { mutableStateListOf<AllocationRowState>() }

    fun rowsFor(currency: Currency) = rows.filter { it.currency == currency }

    fun parsedAmount(row: AllocationRowState) = row.amountText.toBigDecimalOrNullLenient()

    fun isRowValid(row: AllocationRowState): Boolean {
        val toAccount = accounts.find { it.id == row.toAccountId } ?: return false
        val amount = parsedAmount(row) ?: return false
        if (amount.signum() <= 0) return false
        return toAccount.isVirtual || row.fromAccountId != null
    }

    val allocatedByCurrency = surplus.associate { cs ->
        cs.currency to rowsFor(cs.currency).fold(BigDecimal.ZERO) { acc, row -> acc + (parsedAmount(row) ?: BigDecimal.ZERO) }
    }
    val anyRowInvalid = rows.any { !isRowValid(it) }
    val anyCurrencyOverAllocated = surplus.any { cs -> (allocatedByCurrency[cs.currency] ?: BigDecimal.ZERO) > cs.total }
    val canComplete = !anyRowInvalid && !anyCurrencyOverAllocated

    Dialog(onDismissRequest = onSkip, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.budget_surplus_wizard_title)) },
                        navigationIcon = {
                            IconButton(onClick = onSkip) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.budget_surplus_wizard_skip))
                            }
                        }
                    )
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.budget_surplus_wizard_skip))
                        }
                        Button(
                            onClick = { onComplete(rows.filter { isRowValid(it) }.map { it.toDraft() }) },
                            enabled = canComplete,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.budget_surplus_wizard_complete)) }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(surplus, key = { it.currency }) { currencySurplus ->
                        CurrencySurplusSection(
                            currencySurplus = currencySurplus,
                            allocated = allocatedByCurrency[currencySurplus.currency] ?: BigDecimal.ZERO,
                            accounts = accounts,
                            rows = rowsFor(currencySurplus.currency),
                            onAddRow = { rows.add(AllocationRowState(currencySurplus.currency)) },
                            onRemoveRow = { rows.remove(it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A shorter alternative to [OutlinedTextField] — that composable's own minimum height (56dp) can't be
 * overridden via a plain modifier, so this builds the same look (border, floating label, error tint)
 * directly on [BasicTextField] with tighter content padding, letting several of these fit on one row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        readOnly = readOnly,
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            isError = isError,
            label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = trailingIcon,
            contentPadding = OutlinedTextFieldDefaults.contentPadding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
        )
    }
}

/** A tighter alternative to [DropdownMenuItem] — that composable enforces Material's ~48dp list-item minimum height, leaving noticeably more space between rows than these compact dropdowns need. */
@Composable
private fun CompactDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailingIcon?.invoke()
    }
}

private fun AllocationRowState.toDraft() = BudgetDetailViewModel.SurplusAllocationDraft(
    toAccountId = requireNotNull(toAccountId),
    amount = requireNotNull(amountText.toBigDecimalOrNullLenient()),
    fromAccountId = fromAccountId
)

@Composable
private fun CurrencySurplusSection(
    currencySurplus: CurrencySurplus,
    allocated: BigDecimal,
    accounts: List<Account>,
    rows: List<AllocationRowState>,
    onAddRow: () -> Unit,
    onRemoveRow: (AllocationRowState) -> Unit
) {
    val savingAccounts = accounts.filter {
        it.type == AccountType.SAVING && !it.isClosed && it.currency == currencySurplus.currency
    }
    val unallocated = currencySurplus.total - allocated

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            formatMoney(currencySurplus.total, currencySurplus.currency),
            style = MaterialTheme.typography.headlineSmall
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (category in currencySurplus.categories) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category.label, style = MaterialTheme.typography.bodyMedium)
                        Text(formatMoney(category.amount, currencySurplus.currency), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        for (row in rows) {
            AllocationRow(
                row = row,
                savingAccounts = savingAccounts,
                fromAccountCandidates = accounts.filter {
                    !it.isVirtual && !it.isClosed && it.currency == currencySurplus.currency && it.id != row.toAccountId
                },
                onRemove = { onRemoveRow(row) }
            )
        }

        if (savingAccounts.isEmpty()) {
            Text(
                stringResource(R.string.budget_surplus_wizard_no_savings_accounts, currencySurplus.currency.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            TextButton(onClick = onAddRow) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.budget_surplus_wizard_add_allocation))
            }
        }

        Text(
            stringResource(R.string.budget_surplus_wizard_unallocated, formatMoney(unallocated, currencySurplus.currency)),
            style = MaterialTheme.typography.bodySmall,
            color = if (unallocated.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllocationRow(
    row: AllocationRowState,
    savingAccounts: List<Account>,
    fromAccountCandidates: List<Account>,
    onRemove: () -> Unit
) {
    var toMenuExpanded by remember { mutableStateOf(false) }
    var fromMenuExpanded by remember { mutableStateOf(false) }
    val toAccount = savingAccounts.find { it.id == row.toAccountId }
    val amountValid = row.amountText.toBigDecimalOrNullLenient()?.signum()?.let { it > 0 } ?: false

    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1.55f)) {
                    ExposedDropdownMenuBox(expanded = toMenuExpanded, onExpandedChange = { toMenuExpanded = it }) {
                        CompactField(
                            value = toAccount?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = stringResource(R.string.budget_surplus_wizard_to_account),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = toMenuExpanded, onDismissRequest = { toMenuExpanded = false }) {
                            savingAccounts.forEach { candidate ->
                                CompactDropdownMenuItem(
                                    text = candidate.name,
                                    trailingIcon = {
                                        if (candidate.targetReached) {
                                            Icon(
                                                Icons.Filled.CheckCircle,
                                                contentDescription = stringResource(R.string.budget_target_reached_cd),
                                                tint = InvestmentGainColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        row.toAccountId = candidate.id
                                        if (candidate.isVirtual) row.fromAccountId = null
                                        toMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                CompactField(
                    value = row.amountText,
                    onValueChange = { row.amountText = it },
                    label = stringResource(R.string.budget_surplus_wizard_amount),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = row.amountText.isNotBlank() && !amountValid,
                    modifier = Modifier.weight(0.75f)
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.budget_surplus_wizard_remove_allocation))
                }
            }

            toAccount?.let { account ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (account.targetReached) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.budget_target_reached_cd),
                            tint = InvestmentGainColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        stringResource(
                            R.string.budget_currently_amount_target,
                            formatMoney(account.balance, account.currency),
                            account.targetAmount?.let { formatMoney(it, account.currency) }
                                ?: stringResource(R.string.budget_target_not_set)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (toAccount != null && !toAccount.isVirtual) {
                val fromAccount = fromAccountCandidates.find { it.id == row.fromAccountId }
                ExposedDropdownMenuBox(
                    expanded = fromMenuExpanded,
                    onExpandedChange = { fromMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    CompactField(
                        value = fromAccount?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.budget_surplus_wizard_from_account),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = fromMenuExpanded, onDismissRequest = { fromMenuExpanded = false }) {
                        fromAccountCandidates.forEach { candidate ->
                            CompactDropdownMenuItem(
                                text = candidate.name,
                                onClick = {
                                    row.fromAccountId = candidate.id
                                    fromMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
