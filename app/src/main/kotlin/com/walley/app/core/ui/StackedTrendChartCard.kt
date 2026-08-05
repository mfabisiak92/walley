package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ChartHeight = 100.dp
private val ChartHeightExpanded = 260.dp
private val BarWidth = 24.dp

/**
 * A minimal hand-rolled stacked bar chart (no charting library dependency, matching
 * [TrendChartCard]'s approach). Unlike [TrendChartCard]'s grouped bars, every label gets exactly
 * one bar with each series' value stacked bottom-to-top in series order, so the bar's height is
 * governed by the *summed* stack per label rather than any single series' max value. The Y-axis
 * scale stays fixed to the left; tapping a bar reveals that label's breakdown instead of showing
 * totals always.
 */
@Composable
fun StackedTrendChartCard(
    title: String,
    labels: List<String>,
    series: List<ChartSeries>,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) return

    var selectedIndex by remember(labels) { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val stackTotals = labels.indices.map { index ->
        series.sumOf { s -> (s.values.getOrNull(index) ?: 0f).toDouble() }.toFloat()
    }
    val rawMaxTotal = stackTotals.maxOrNull()?.coerceAtLeast(0f) ?: 0f
    val ticks = niceTicks(0f, rawMaxTotal)
    val maxTotal = ticks.last()

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
            StackedTrendChartBody(
                labels = labels,
                series = series,
                valueFormatter = valueFormatter,
                ticks = ticks,
                stackTotals = stackTotals,
                maxTotal = maxTotal,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                chartHeight = ChartHeight
            )
        }
    }

    if (expanded) {
        FullScreenChartDialog(title = title, onDismiss = { expanded = false }) {
            StackedTrendChartBody(
                labels = labels,
                series = series,
                valueFormatter = valueFormatter,
                ticks = ticks,
                stackTotals = stackTotals,
                maxTotal = maxTotal,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                chartHeight = ChartHeightExpanded
            )
        }
    }
}

@Composable
private fun StackedTrendChartBody(
    labels: List<String>,
    series: List<ChartSeries>,
    valueFormatter: (Float) -> String,
    ticks: List<Float>,
    stackTotals: List<Float>,
    maxTotal: Float,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    chartHeight: Dp
) {
    Column {
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

        selectedIndex?.let { index ->
            Text(
                selectedStackDescription(labels[index], series, stackTotals[index], index, valueFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(modifier = Modifier.padding(top = 16.dp)) {
            ChartAxisScale(ticks = ticks, height = chartHeight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                labels.forEachIndexed { index, label ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onSelect(if (selectedIndex == index) null else index)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .height(chartHeight)
                                .background(
                                    if (selectedIndex == index) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        Color.Transparent
                                    }
                                ),
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
                                    val segmentHeight = chartHeight * (value / maxTotal).coerceIn(0f, 1f)
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

/** "Label — Total: total (series1: value1, series2: value2)". */
private fun selectedStackDescription(
    label: String,
    series: List<ChartSeries>,
    total: Float,
    index: Int,
    valueFormatter: (Float) -> String
): String {
    val breakdown = series.joinToString(", ") { s ->
        "${s.name}: ${valueFormatter((s.values.getOrNull(index) ?: 0f).coerceAtLeast(0f))}"
    }
    return "$label — Total: ${valueFormatter(total)} ($breakdown)"
}
