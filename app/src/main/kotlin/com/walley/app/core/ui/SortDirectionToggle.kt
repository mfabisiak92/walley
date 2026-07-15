package com.walley.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.SortDirection

/**
 * Compact ascending/descending icon toggle for a sort field's direction. A text-labeled segmented
 * button pair (e.g. "Best→Worst" / "Worst→Best") doesn't reliably fit next to a field's radio row on a
 * phone-width sheet, so this uses plain up/down arrows instead — [ascendingDescription] and
 * [descendingDescription] carry the field-specific meaning for screen readers only.
 */
@Composable
fun SortDirectionToggle(
    direction: SortDirection,
    ascendingDescription: String,
    descendingDescription: String,
    onDirectionSelected: (SortDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilledIconToggleButton(
            checked = direction == SortDirection.ASC,
            onCheckedChange = { onDirectionSelected(SortDirection.ASC) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Filled.ArrowUpward, contentDescription = ascendingDescription, modifier = Modifier.size(16.dp))
        }
        FilledIconToggleButton(
            checked = direction == SortDirection.DESC,
            onCheckedChange = { onDirectionSelected(SortDirection.DESC) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Filled.ArrowDownward, contentDescription = descendingDescription, modifier = Modifier.size(16.dp))
        }
    }
}
