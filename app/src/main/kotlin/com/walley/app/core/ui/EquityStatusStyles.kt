package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.walley.app.domain.model.EquityStatus

val EquityStatusColors: Map<EquityStatus, Color> = mapOf(
    EquityStatus.BUY to Color(0xFF2E7D32),
    EquityStatus.SELL to Color(0xFFC62828),
    EquityStatus.HOLD to Color(0xFF1565C0),
    EquityStatus.WAIT to Color(0xFFEF6C00)
)

private val StatusBadgeWidth = 64.dp

/**
 * Bold, solid-fill pill showing an equity status label — deliberately high-contrast/visible.
 * Always the same fixed width, so badges line up regardless of which status they show.
 */
@Composable
fun EquityStatusBadge(status: EquityStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(StatusBadgeWidth)
            .clip(RoundedCornerShape(50))
            .background(EquityStatusColors.getValue(status))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

/** Shows a status change as two badges joined by an arrow, e.g. "HOLD → BUY". */
@Composable
fun EquityStatusTransitionBadge(previous: EquityStatus, latest: EquityStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        EquityStatusBadge(status = previous)
        Text(
            "→",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        EquityStatusBadge(status = latest)
    }
}
