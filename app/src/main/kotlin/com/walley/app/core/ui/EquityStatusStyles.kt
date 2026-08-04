package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.EquityStatus
import com.walley.app.domain.model.displayName

val EquityStatusColors: Map<EquityStatus, Color> = mapOf(
    EquityStatus.BUY to Color(0xFF2E7D32),
    EquityStatus.SELL to Color(0xFFC62828),
    EquityStatus.HOLD to Color(0xFF1565C0),
    EquityStatus.WAIT to Color(0xFFEF6C00)
)

private val EquityStatusIcons: Map<EquityStatus, ImageVector> = mapOf(
    EquityStatus.BUY to Icons.AutoMirrored.Filled.TrendingUp,
    EquityStatus.SELL to Icons.AutoMirrored.Filled.TrendingDown,
    EquityStatus.HOLD to Icons.Filled.Pause,
    EquityStatus.WAIT to Icons.Filled.AccessTime
)

/** Small circular colored badge showing an equity's status as an icon, matching [BudgetItemIconBadge]'s style. */
@Composable
fun EquityStatusIconBadge(status: EquityStatus, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val color = EquityStatusColors.getValue(status)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            EquityStatusIcons.getValue(status),
            contentDescription = status.displayName(),
            tint = color,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/** A small pill labeling a linked strategy's status — same visual treatment as [InvestmentCategoryChip], for placing inline next to it. */
@Composable
fun EquityStatusChip(status: EquityStatus, modifier: Modifier = Modifier) {
    val color = EquityStatusColors.getValue(status)
    Text(
        text = status.displayName(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
