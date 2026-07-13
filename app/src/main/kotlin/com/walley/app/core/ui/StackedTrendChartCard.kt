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
import androidx.compose.ui.unit.dp

private val ChartHeight = 100.dp
private val BarWidth = 24.dp

/**
 * A minimal hand-rolled stacked bar chart (no charting library dependency, matching
 * [TrendChartCard]'s approach). Unlike [TrendChartCard]'s grouped bars, every label gets exactly
 * one bar with each series' value stacked bottom-to-top in series order, so the bar's height is
 * governed by the *summed* stack per label rather than any single series' max value.
 */
@Composable
fun StackedTrendChartCard(
    title: String,
    labels: List<String>,
    series: List<ChartSeries>,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier,
    showTotalLabels: Boolean = false
) {
    if (labels.isEmpty()) return

    val stackTotals = labels.indices.map { index ->
        series.sumOf { s -> (s.values.getOrNull(index) ?: 0f).toDouble() }.toFloat()
    }
    val maxTotal = stackTotals.maxOrNull()?.coerceAtLeast(1f) ?: 1f

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
                        .horizontalScroll(rememberScrollState())
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
                        if (showTotalLabels) {
                            Text(
                                valueFormatter(stackTotals[index]),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier.height(ChartHeight),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Reversed so the first series ends up visually at the bottom of the stack.
                                series.asReversed().forEach { s ->
                                    val value = (s.values.getOrNull(index) ?: 0f).coerceAtLeast(0f)
                                    if (value <= 0f) return@forEach
                                    val segmentHeight = ChartHeight * (value / maxTotal).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .width(BarWidth)
                                            .height(segmentHeight.coerceAtLeast(2.dp))
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(s.color)
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
