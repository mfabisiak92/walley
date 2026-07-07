package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.BudgetItemIcon

/** Small circular colored badge showing a budget item's icon, or nothing if it has none. */
@Composable
fun BudgetItemIconBadge(icon: BudgetItemIcon?, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 32.dp) {
    if (icon == null) return
    val style = BudgetItemIconStyles.getValue(icon)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(style.color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            style.vector,
            contentDescription = icon.label,
            tint = style.color,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}
