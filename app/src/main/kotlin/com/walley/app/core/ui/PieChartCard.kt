package com.walley.app.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

val PieChartColors = listOf(
    Color(0xFF00695C),
    Color(0xFFFFB74D),
    Color(0xFF1565C0),
    Color(0xFFC62828),
    Color(0xFF6A1B9A),
    Color(0xFF2E7D32)
)

data class PieSlice(
    val label: String,
    val value: String,
    val percent: Float,
    val color: Color
)

private val PieSize = 120.dp
private val PieSizeExpanded = 220.dp

@Composable
fun PieChartCard(title: String, slices: List<PieSlice>, modifier: Modifier = Modifier) {
    if (slices.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                ExpandChartIconButton(onClick = { expanded = true })
            }
            PieChartBody(slices, PieSize, Modifier.padding(top = 12.dp))
        }
    }

    if (expanded) {
        FullScreenChartDialog(title = title, onDismiss = { expanded = false }) {
            PieChartBody(slices, PieSizeExpanded)
        }
    }
}

@Composable
private fun PieChartBody(slices: List<PieSlice>, pieSize: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(modifier = Modifier.size(pieSize)) {
            Canvas(modifier = Modifier.fillMaxWidth().size(pieSize)) {
                var startAngle = -90f
                slices.forEach { slice ->
                    val sweep = slice.percent / 100f * 360f
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        size = Size(size.width, size.height)
                    )
                    startAngle += sweep
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // The gap before the label comes entirely from the Spacer above, not leading
                    // spaces baked into the string — those only offset the first line, so a
                    // wrapped second line would start further left than the first, misaligned.
                    Column {
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${slice.value} · ${String.format(Locale.US, "%.2f", slice.percent)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
