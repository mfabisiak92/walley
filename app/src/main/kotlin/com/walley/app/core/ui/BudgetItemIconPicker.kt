package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.BudgetItemIcon

/**
 * A horizontally-scrollable row of selectable icon chips, plus a "None" option. Shows the
 * currently selected icon's name above the row and auto-scrolls it into view, since with 30+
 * icons a newly-selected one (e.g. from name-based auto-suggestion) can otherwise land off-screen
 * and go unnoticed.
 */
@Composable
fun BudgetItemIconPicker(
    options: List<BudgetItemIcon>,
    selected: BudgetItemIcon?,
    onSelect: (BudgetItemIcon?) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = if (selected == null) 0 else options.indexOf(selected) + 1

    LaunchedEffect(selected) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = selected?.label ?: "No icon",
            style = MaterialTheme.typography.bodyMedium,
            color = selected?.let { BudgetItemIconStyles.getValue(it).color } ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { NoneIconChip(isSelected = selected == null, onClick = { onSelect(null) }) }
            items(options) { icon ->
                IconChip(icon = icon, isSelected = selected == icon, onClick = { onSelect(icon) })
            }
        }
    }
}

@Composable
private fun IconChip(icon: BudgetItemIcon, isSelected: Boolean, onClick: () -> Unit) {
    val style = BudgetItemIconStyles.getValue(icon)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(style.color.copy(alpha = if (isSelected) 0.3f else 0.12f))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) style.color else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(style.vector, contentDescription = icon.label, tint = style.color, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun NoneIconChip(isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Close, contentDescription = "None", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
