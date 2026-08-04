package com.walley.app.feature.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.walley.app.R
import com.walley.app.core.ui.SortDirectionToggle
import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.SortDirection
import com.walley.app.domain.model.displayName

/**
 * Single combined sheet for both sort and filter, opened from the funnel icon in [com.walley.app.core.ui.WalleyTopBar].
 * Every control applies immediately (no separate Apply step) and is persisted by the caller through
 * [com.walley.app.feature.accounts.AccountsViewModel] — this composable is purely presentational.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsSortFilterSheet(
    group: AccountBalanceGroup,
    sortState: AccountsSortState,
    filterState: AccountsFilterState,
    availableCurrencies: List<Currency>,
    onSortFieldSelected: (AccountSortField) -> Unit,
    onSortDirectionSelected: (SortDirection) -> Unit,
    onStatusSelected: (AccountStatusFilter) -> Unit,
    onCurrencyToggled: (Currency) -> Unit,
    onTypeToggled: (AccountType) -> Unit,
    onKindSelected: (AccountKindFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(stringResource(R.string.accounts_sort_filter_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.accounts_sort_filter_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            SectionLabel(stringResource(R.string.accounts_sort_by))
            SortFieldRow(
                AccountSortField.NAME,
                stringResource(R.string.accounts_name),
                stringResource(R.string.accounts_sort_asc_az),
                stringResource(R.string.accounts_sort_desc_za),
                sortState,
                onSortFieldSelected,
                onSortDirectionSelected
            )
            SortFieldRow(
                AccountSortField.BALANCE,
                stringResource(R.string.accounts_balance),
                stringResource(R.string.accounts_sort_asc_low_high),
                stringResource(R.string.accounts_sort_desc_high_low),
                sortState,
                onSortFieldSelected,
                onSortDirectionSelected
            )
            SortFieldRow(
                AccountSortField.DATE_ADDED,
                stringResource(R.string.accounts_date_added),
                stringResource(R.string.accounts_sort_asc_oldest_first),
                stringResource(R.string.accounts_sort_desc_newest_first),
                sortState,
                onSortFieldSelected,
                onSortDirectionSelected
            )
            SortFieldRow(
                AccountSortField.DEFAULT_FIRST,
                stringResource(R.string.accounts_sort_field_default_account_first),
                null,
                null,
                sortState,
                onSortFieldSelected,
                onSortDirectionSelected
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionLabel(stringResource(R.string.accounts_status))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                val options = listOf(
                    AccountStatusFilter.ACTIVE to stringResource(R.string.accounts_status_active),
                    AccountStatusFilter.CLOSED to stringResource(R.string.accounts_closed),
                    AccountStatusFilter.ALL to stringResource(R.string.accounts_all)
                )
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = filterState.status == value,
                        onClick = { onStatusSelected(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) { Text(label) }
                }
            }

            if (availableCurrencies.size > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                SectionLabel(stringResource(R.string.accounts_currency))
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCurrencies.forEach { currency ->
                        FilterChip(
                            selected = currency in filterState.currencies,
                            onClick = { onCurrencyToggled(currency) },
                            label = { Text(currency.name) }
                        )
                    }
                }
            }

            // Only Cash & Checking mixes more than one AccountType, so a type filter is meaningless elsewhere.
            if (group == AccountBalanceGroup.CASH_CHECKING) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                SectionLabel(stringResource(R.string.accounts_account_type))
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.types.forEach { type ->
                        FilterChip(
                            selected = type in filterState.types,
                            onClick = { onTypeToggled(type) },
                            label = { Text(type.displayName()) }
                        )
                    }
                }
            }

            // Only Savings mixes real and virtual (earmarked) accounts, so kind only makes sense here.
            if (group == AccountBalanceGroup.SAVINGS) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                SectionLabel(stringResource(R.string.accounts_kind))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    val options = listOf(
                        AccountKindFilter.ALL to stringResource(R.string.accounts_all),
                        AccountKindFilter.REAL to stringResource(R.string.accounts_real),
                        AccountKindFilter.VIRTUAL to stringResource(R.string.accounts_virtual)
                    )
                    options.forEachIndexed { index, (value, label) ->
                        SegmentedButton(
                            selected = filterState.kind == value,
                            onClick = { onKindSelected(value) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                        ) { Text(label) }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onReset) { Text(stringResource(R.string.accounts_reset)) }
                Button(onClick = onDismiss) { Text(stringResource(R.string.accounts_done)) }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SortFieldRow(
    field: AccountSortField,
    label: String,
    ascendingDescription: String?,
    descendingDescription: String?,
    sortState: AccountsSortState,
    onFieldSelected: (AccountSortField) -> Unit,
    onDirectionSelected: (SortDirection) -> Unit
) {
    val selected = sortState.field == field
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFieldSelected(field) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = { onFieldSelected(field) })
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        if (selected && ascendingDescription != null && descendingDescription != null) {
            SortDirectionToggle(
                direction = sortState.direction,
                ascendingDescription = ascendingDescription,
                descendingDescription = descendingDescription,
                onDirectionSelected = onDirectionSelected
            )
        }
    }
}
