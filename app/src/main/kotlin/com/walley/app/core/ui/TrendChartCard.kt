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
import androidx.compose.foundation.layout.offset
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class ChartSeries(
    val name: String,
    val color: Color,
    /** One entry per label, parallel to [TrendChartCard]'s `labels`; null means no data for that point. */
    val values: List<Float?>
)

private val ChartHeight = 100.dp
private val ChartHeightExpanded = 260.dp
private val BarWidth = 10.dp
private val AxisWidth = 44.dp

/**
 * A minimal hand-rolled grouped bar chart (no charting library dependency, matching [PieChartCard]'s approach).
 * Horizontally scrollable so it stays readable regardless of how many data points there are. The Y-axis scale
 * stays fixed to the left; tapping a bar column reveals that point's value(s) instead of showing them always.
 */
@Composable
fun TrendChartCard(
    title: String,
    labels: List<String>,
    series: List<ChartSeries>,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) return

    var selectedIndex by remember(labels) { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val allValues = series.flatMap { it.values }.filterNotNull()
    val rawMax = (allValues.maxOrNull() ?: 0f).coerceAtLeast(0f)
    val rawMin = (allValues.minOrNull() ?: 0f).coerceAtMost(0f)
    val ticks = niceTicks(rawMin, rawMax)
    val minValue = ticks.first()
    val maxValue = ticks.last()
    // Where the zero line sits, as a fraction of the chart height measured from the top — used to
    // grow bars up from zero for positive values and down from zero for negative ones.
    val zeroFraction = yFraction(0f, minValue, maxValue)

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
            TrendChartBody(
                labels = labels,
                series = series,
                valueFormatter = valueFormatter,
                ticks = ticks,
                minValue = minValue,
                maxValue = maxValue,
                zeroFraction = zeroFraction,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                chartHeight = ChartHeight
            )
        }
    }

    if (expanded) {
        FullScreenChartDialog(title = title, onDismiss = { expanded = false }) {
            TrendChartBody(
                labels = labels,
                series = series,
                valueFormatter = valueFormatter,
                ticks = ticks,
                minValue = minValue,
                maxValue = maxValue,
                zeroFraction = zeroFraction,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                chartHeight = ChartHeightExpanded
            )
        }
    }
}

@Composable
private fun TrendChartBody(
    labels: List<String>,
    series: List<ChartSeries>,
    valueFormatter: (Float) -> String,
    ticks: List<Float>,
    minValue: Float,
    maxValue: Float,
    zeroFraction: Float,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    chartHeight: Dp
) {
    Column {
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

        selectedIndex?.let { index ->
            Text(
                selectedPointDescription(labels[index], series, index, valueFormatter),
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
                                )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                series.forEach { s ->
                                    val value = s.values.getOrNull(index)
                                    // A fixed-size placeholder the full chart height, so every column lines
                                    // up regardless of sign; the actual bar is positioned inside it by offset.
                                    Box(
                                        modifier = Modifier
                                            .width(BarWidth)
                                            .height(chartHeight)
                                    ) {
                                        val valueFraction = yFraction(value ?: 0f, minValue, maxValue)
                                        val topFraction = if (value != null) minOf(valueFraction, zeroFraction) else zeroFraction
                                        val bottomFraction = if (value != null) maxOf(valueFraction, zeroFraction) else zeroFraction
                                        val barTop = chartHeight * topFraction
                                        val barHeight = (chartHeight * (bottomFraction - topFraction)).coerceAtLeast(2.dp)
                                        Box(
                                            modifier = Modifier
                                                .offset(y = barTop)
                                                .width(BarWidth)
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (value != null) s.color else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                        )
                                    }
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

/**
 * Fraction of a chart's height, measured from the top, at which [value] falls within the visible
 * [minValue]..[maxValue] range — 0 at the top ([maxValue]), 1 at the bottom ([minValue]). Used to place
 * a bar's top/bottom edges (or a line chart's point) regardless of where zero sits in that range.
 */
private fun yFraction(value: Float, minValue: Float, maxValue: Float): Float {
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    return ((maxValue - value) / range).coerceIn(0f, 1f)
}

/**
 * A vertical scale of evenly-spaced [ticks] (highest first), aligned to a chart area of the given [height].
 * [ticks] is expected to come from [niceTicks], so consecutive entries are equally spaced in value — that
 * lets a plain [Arrangement.SpaceBetween] double as correct proportional vertical placement.
 *
 * Labels always use [formatCompactAxisValue] rather than the chart's full `valueFormatter` (which is still
 * used for the tap-to-reveal description) — a fixed-width axis column has no room for a fully formatted
 * value like "50,000.00 zł" once the chart's range gets large, and it was getting clipped.
 */
@Composable
internal fun ChartAxisScale(
    ticks: List<Float>,
    height: Dp
) {
    Column(
        modifier = Modifier
            .width(AxisWidth)
            .height(height),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ticks.asReversed().forEach { tick ->
            Text(
                formatCompactAxisValue(tick),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Compact form of a round axis tick value: 1000 -> "1k", 10000 -> "10k", 1000000 -> "1M", etc. Ticks come
 * from [niceTicks] so they're always "nice" round numbers, meaning a single optional decimal digit is
 * always enough precision (no ticks like 1234 ever reach here).
 */
internal fun formatCompactAxisValue(value: Float): String {
    val absValue = abs(value)
    val (divisor, suffix) = when {
        absValue >= 1_000_000_000f -> 1_000_000_000f to "B"
        absValue >= 1_000_000f -> 1_000_000f to "M"
        absValue >= 1_000f -> 1_000f to "k"
        else -> 1f to ""
    }
    val scaled = value / divisor
    val rounded = (scaled * 10f).roundToInt() / 10f
    val text = if (rounded == rounded.toLong().toFloat()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
    return "$text$suffix"
}

/**
 * "Nice" round tick values (e.g. 0, 50, 100, 150, 200 — never 0, 37, 74, 111, ...) spanning at least
 * [min]..[max], ascending. Uses the standard nicenum/loose-label algorithm (Heckbert), so steps always
 * land on 1, 2, or 5 × a power of ten.
 */
internal fun niceTicks(min: Float, max: Float, targetTicks: Int = 5): List<Float> {
    val safeMax = if (max <= min) min + 1f else max
    val range = niceNum(safeMax - min, round = false)
    val step = niceNum(range / (targetTicks - 1).coerceAtLeast(1), round = true)
    val niceMin = floor(min / step) * step
    val niceMax = ceil(safeMax / step) * step
    val ticks = mutableListOf<Float>()
    var v = niceMin
    // A small epsilon guards against float accumulation drift excluding the last tick.
    while (v <= niceMax + step * 0.001f) {
        ticks.add(v)
        v += step
    }
    return ticks
}

/** Rounds [range] to a "nice" number: 1, 2, 5, or 10 times a power of ten. */
private fun niceNum(range: Float, round: Boolean): Float {
    if (range <= 0f) return 1f
    val exponent = floor(log10(range.toDouble())).toInt()
    val fraction = range / 10f.pow(exponent)
    val niceFraction = if (round) {
        when {
            fraction < 1.5f -> 1f
            fraction < 3f -> 2f
            fraction < 7f -> 5f
            else -> 10f
        }
    } else {
        when {
            fraction <= 1f -> 1f
            fraction <= 2f -> 2f
            fraction <= 5f -> 5f
            else -> 10f
        }
    }
    return niceFraction * 10f.pow(exponent)
}

/** "Label — series1: value1, series2: value2" (or just "Label: value" for a single series). */
internal fun selectedPointDescription(
    label: String,
    series: List<ChartSeries>,
    index: Int,
    valueFormatter: (Float) -> String
): String {
    if (series.size <= 1) {
        val value = series.firstOrNull()?.values?.getOrNull(index)
        return "$label: ${value?.let(valueFormatter) ?: "—"}"
    }
    val parts = series.joinToString(", ") { s ->
        val value = s.values.getOrNull(index)
        "${s.name}: ${value?.let(valueFormatter) ?: "—"}"
    }
    return "$label — $parts"
}
