package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.BudgetItemIcon

/** A horizontally-scrollable row of selectable icon chips, plus a "None" option. */
@Composable
fun BudgetItemIconPicker(
    options: List<BudgetItemIcon>,
    selected: BudgetItemIcon?,
    onSelect: (BudgetItemIcon?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NoneIconChip(isSelected = selected == null, onClick = { onSelect(null) })
        options.forEach { icon ->
            IconChip(icon = icon, isSelected = selected == icon, onClick = { onSelect(icon) })
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
