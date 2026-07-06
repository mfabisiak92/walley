package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ChartSeries(
    val name: String,
    val color: Color,
    /** One entry per label, parallel to [TrendChartCard]'s `labels`; null means no data for that point. */
    val values: List<Float?>
)

private val ChartHeight = 100.dp
private val BarWidth = 10.dp

/**
 * A minimal hand-rolled grouped bar chart (no charting library dependency, matching [PieChartCard]'s approach).
 * Horizontally scrollable so it stays readable regardless of how many data points there are.
 */
@Composable
fun TrendChartCard(
    title: String,
    labels: List<String>,
    series: List<ChartSeries>,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier,
    showValueLabels: Boolean = false
) {
    if (labels.isEmpty()) return

    val maxValue = series.flatMap { it.values }.filterNotNull().maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)

            if (series.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    series.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(s.color)
                            )
                            Text(
                                "  ${s.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                labels.forEachIndexed { index, label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (showValueLabels) {
                            val value = series.firstOrNull()?.values?.getOrNull(index)
                            Text(
                                value?.let(valueFormatter) ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier.height(ChartHeight),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                series.forEach { s ->
                                    val value = s.values.getOrNull(index)
                                    val barHeight = if (value != null) {
                                        (ChartHeight * (value / maxValue).coerceIn(0f, 1f))
                                    } else {
                                        2.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(BarWidth)
                                            .height(barHeight.coerceAtLeast(2.dp))
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (value != null) s.color else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )
                                }
                            }
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
