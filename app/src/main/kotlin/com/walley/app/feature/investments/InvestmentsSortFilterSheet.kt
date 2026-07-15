package com.walley.app.feature.investments

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
import androidx.compose.ui.unit.dp
import com.walley.app.core.ui.SortDirectionToggle
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentSortField
import com.walley.app.domain.model.InvestmentsFilterState
import com.walley.app.domain.model.InvestmentsSortState
import com.walley.app.domain.model.PositionStatusFilter
import com.walley.app.domain.model.SortDirection

/**
 * Combined sort/filter sheet for the Portfolio list, opened from the funnel icon next to the tabs in
 * [InvestmentsScreen]. Mirrors [com.walley.app.feature.accounts.AccountsSortFilterSheet]'s shape and
 * live-apply/persisted behavior — every control applies immediately, persistence lives in the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsSortFilterSheet(
    sortState: InvestmentsSortState,
    filterState: InvestmentsFilterState,
    availableCategories: List<InvestmentCategory>,
    availableCurrencies: List<Currency>,
    availableAccounts: List<Account>,
    onSortFieldSelected: (InvestmentSortField) -> Unit,
    onSortDirectionSelected: (SortDirection) -> Unit,
    onStatusSelected: (PositionStatusFilter) -> Unit,
    onCategoryToggled: (InvestmentCategory) -> Unit,
    onCurrencyToggled: (Currency) -> Unit,
    onAccountToggled: (Long) -> Unit,
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
            Text("Sort & filter", style = MaterialTheme.typography.titleLarge)
            Text(
                "Changes apply immediately and are remembered next time you open Portfolio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            SectionLabel("Sort by")
            SortFieldRow(InvestmentSortField.NAME, "Name", "A→Z", "Z→A", sortState, onSortFieldSelected, onSortDirectionSelected)
            SortFieldRow(InvestmentSortField.VALUE, "Value", "Low→High", "High→Low", sortState, onSortFieldSelected, onSortDirectionSelected)
            SortFieldRow(InvestmentSortField.GAIN_LOSS_PERCENT, "Gain/loss %", "Worst first", "Best first", sortState, onSortFieldSelected, onSortDirectionSelected)
            SortFieldRow(InvestmentSortField.DATE_ADDED, "Date added", "Oldest first", "Newest first", sortState, onSortFieldSelected, onSortDirectionSelected)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SectionLabel("Status")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                val options = listOf(
                    PositionStatusFilter.OPEN to "Open",
                    PositionStatusFilter.CLOSED to "Closed",
                    PositionStatusFilter.ALL to "All"
                )
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = filterState.status == value,
                        onClick = { onStatusSelected(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) { Text(label) }
                }
            }

            if (availableCategories.size > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                SectionLabel("Category")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { category ->
                        FilterChip(
                            selected = category in filterState.categories,
                            onClick = { onCategoryToggled(category) },
                            label = { Text(category.label) }
                        )
                    }
                }
            }

            if (availableCurrencies.size > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                SectionLabel("Currency")
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

            if (availableAccounts.size > 1) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                SectionLabel("Account")
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableAccounts.forEach { account ->
                        FilterChip(
                            selected = account.id in filterState.accountIds,
                            onClick = { onAccountToggled(account.id) },
                            label = { Text(account.name) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onReset) { Text("Reset") }
                Button(onClick = onDismiss) { Text("Done") }
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
    field: InvestmentSortField,
    label: String,
    ascendingDescription: String,
    descendingDescription: String,
    sortState: InvestmentsSortState,
    onFieldSelected: (InvestmentSortField) -> Unit,
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
        if (selected) {
            SortDirectionToggle(
                direction = sortState.direction,
                ascendingDescription = ascendingDescription,
                descendingDescription = descendingDescription,
                onDirectionSelected = onDirectionSelected
            )
        }
    }
}
