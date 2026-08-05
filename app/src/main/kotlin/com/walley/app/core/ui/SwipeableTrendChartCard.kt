package com.walley.app.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ChartType(val label: String) { AREA("Area"), LINE("Line") }

private val ChartHeight = 140.dp
private val ChartHeightExpanded = 320.dp
private val SwipeThreshold = 56.dp
private const val MAX_TEXT_LABELS = 6

/**
 * A hand-rolled line/area trend chart (no charting library, matching [TrendChartCard]'s bar-chart
 * approach). Defaults to an area chart; swiping left or right anywhere on the chart toggles it to
 * a line chart and back, independently per card. Tapping (a clean down-up with negligible
 * movement, which the swipe gesture never consumes) selects the nearest data point and reveals
 * its value instead of showing every point's value always.
 */
@Composable
fun SwipeableTrendChartCard(
    title: String,
    labels: List<String>,
    series: List<ChartSeries>,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) return

    var chartType by remember { mutableStateOf(ChartType.AREA) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember(labels) { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { SwipeThreshold.toPx() }

    val allValues = series.flatMap { it.values }.filterNotNull()
    val rawMax = (allValues.maxOrNull() ?: 0f).coerceAtLeast(0f)
    val rawMin = (allValues.minOrNull() ?: 0f).coerceAtMost(0f)
    val ticks = niceTicks(rawMin, rawMax)
    val minValue = ticks.first()
    val maxValue = ticks.last()
    val labelIndices = remember(labels.size) { selectLabelIndices(labels.size) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "  ${chartType.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExpandChartIconButton(onClick = { expanded = true })
                }
            }

            SwipeableTrendChartBody(
                labels = labels,
                series = series,
                valueFormatter = valueFormatter,
                ticks = ticks,
                minValue = minValue,
                maxValue = maxValue,
                labelIndices = labelIndices,
                chartType = chartType,
                onChartTypeChange = { chartType = it },
                dragAccumulator = dragAccumulator,
                onDragAccumulatorChange = { dragAccumulator = it },
                swipeThresholdPx = swipeThresholdPx,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                chartHeight = ChartHeight
            )
        }
    }

    if (expanded) {
        FullScreenChartDialog(title = title, onDismiss = { expanded = false }) {
            SwipeableTrendChartBody(
                labels = labels,
                series = series,
                valueFormatter = valueFormatter,
                ticks = ticks,
                minValue = minValue,
                maxValue = maxValue,
                labelIndices = labelIndices,
                chartType = chartType,
                onChartTypeChange = { chartType = it },
                dragAccumulator = dragAccumulator,
                onDragAccumulatorChange = { dragAccumulator = it },
                swipeThresholdPx = swipeThresholdPx,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                chartHeight = ChartHeightExpanded
            )
        }
    }
}

@Composable
private fun SwipeableTrendChartBody(
    labels: List<String>,
    series: List<ChartSeries>,
    valueFormatter: (Float) -> String,
    ticks: List<Float>,
    minValue: Float,
    maxValue: Float,
    labelIndices: List<Int>,
    chartType: ChartType,
    onChartTypeChange: (ChartType) -> Unit,
    dragAccumulator: Float,
    onDragAccumulatorChange: (Float) -> Unit,
    swipeThresholdPx: Float,
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

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = { onDragAccumulatorChange(0f) },
                                onDragCancel = { onDragAccumulatorChange(0f) },
                                onHorizontalDrag = { change, dragAmount ->
                                    val newAccumulator = dragAccumulator + dragAmount
                                    if (abs(newAccumulator) > swipeThresholdPx) {
                                        onChartTypeChange(if (chartType == ChartType.AREA) ChartType.LINE else ChartType.AREA)
                                        onDragAccumulatorChange(0f)
                                    } else {
                                        onDragAccumulatorChange(newAccumulator)
                                    }
                                    change.consume()
                                }
                            )
                        }
                        .pointerInput(labels) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val stepX = if (labels.size > 1) size.width.toFloat() / (labels.size - 1) else 0f
                                    val index = if (stepX > 0f) {
                                        (offset.x / stepX).roundToInt().coerceIn(0, labels.size - 1)
                                    } else {
                                        0
                                    }
                                    onSelect(if (selectedIndex == index) null else index)
                                }
                            )
                        }
                ) {
                    series.forEach { s ->
                        drawTrendSeries(
                            values = s.values,
                            color = s.color,
                            minValue = minValue,
                            maxValue = maxValue,
                            filled = chartType == ChartType.AREA
                        )
                    }
                    selectedIndex?.let { index ->
                        drawSelectionGuide(index, labels.size, series, minValue, maxValue)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labelIndices.forEach { index ->
                        Text(
                            labels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** At most [MAX_TEXT_LABELS] evenly-spaced indices, so labels never overlap regardless of point count. */
private fun selectLabelIndices(count: Int, maxLabels: Int = MAX_TEXT_LABELS): List<Int> {
    if (count <= maxLabels) return (0 until count).toList()
    val step = (count - 1).toFloat() / (maxLabels - 1)
    return (0 until maxLabels).map { i -> (i * step).roundToInt() }.distinct()
}

private fun DrawScope.drawTrendSeries(
    values: List<Float?>,
    color: Color,
    minValue: Float,
    maxValue: Float,
    filled: Boolean
) {
    val n = values.size
    if (n == 0) return
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    val stepX = if (n > 1) size.width / (n - 1) else 0f

    fun xFor(index: Int) = index * stepX
    fun yFor(value: Float) = size.height - ((value - minValue) / range) * size.height

    var runStart = -1
    for (i in 0 until n) {
        if (values[i] != null) {
            if (runStart == -1) runStart = i
        } else {
            if (runStart != -1) {
                drawRun(values, runStart, i - 1, ::xFor, ::yFor, color, filled)
            }
            runStart = -1
        }
    }
    if (runStart != -1) {
        drawRun(values, runStart, n - 1, ::xFor, ::yFor, color, filled)
    }
}

private fun DrawScope.drawRun(
    values: List<Float?>,
    start: Int,
    end: Int,
    xFor: (Int) -> Float,
    yFor: (Float) -> Float,
    color: Color,
    filled: Boolean
) {
    if (end <= start) {
        drawCircle(color, radius = 3.dp.toPx(), center = Offset(xFor(start), yFor(values[start]!!)))
        return
    }
    val linePath = Path().apply {
        moveTo(xFor(start), yFor(values[start]!!))
        for (i in start + 1..end) {
            lineTo(xFor(i), yFor(values[i]!!))
        }
    }
    if (filled) {
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(xFor(end), size.height)
            lineTo(xFor(start), size.height)
            close()
        }
        drawPath(fillPath, color.copy(alpha = 0.22f))
    }
    drawPath(
        linePath,
        color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/** A vertical guide line at the selected index, plus a dot per series that has a value there. */
private fun DrawScope.drawSelectionGuide(
    index: Int,
    labelCount: Int,
    series: List<ChartSeries>,
    minValue: Float,
    maxValue: Float
) {
    val stepX = if (labelCount > 1) size.width / (labelCount - 1) else 0f
    val x = index * stepX
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    fun yFor(value: Float) = size.height - ((value - minValue) / range) * size.height

    drawLine(
        color = Color.Gray.copy(alpha = 0.4f),
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1.dp.toPx()
    )
    series.forEach { s ->
        val value = s.values.getOrNull(index) ?: return@forEach
        drawCircle(s.color, radius = 4.dp.toPx(), center = Offset(x, yFor(value)))
    }
}
