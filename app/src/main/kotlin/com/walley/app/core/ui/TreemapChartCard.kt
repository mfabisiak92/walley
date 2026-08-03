package com.walley.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class TreemapItem(
    val label: String,
    val displayValue: String,
    val value: Float,
    val color: Color
)

private data class TreemapRect(val x: Float, val y: Float, val width: Float, val height: Float)

private val TreemapHeight = 240.dp
private const val MinTileWidthForLabel = 56f
private const val MinTileHeightForLabel = 32f
private const val MinTileHeightForValue = 48f

/** A card showing [items] as a squarified treemap — each tile's area proportional to its [TreemapItem.value]. */
@Composable
fun TreemapChartCard(title: String, items: List<TreemapItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    val sortedItems = remember(items) { items.sortedByDescending { it.value } }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TreemapHeight)
                    .padding(top = 12.dp)
            ) {
                val containerWidth = maxWidth.value
                val containerHeight = maxHeight.value
                val rects = remember(sortedItems, containerWidth, containerHeight) {
                    squarify(sortedItems.map { it.value }, TreemapRect(0f, 0f, containerWidth, containerHeight))
                }
                sortedItems.forEachIndexed { index, item ->
                    val rect = rects.getOrNull(index) ?: return@forEachIndexed
                    if (rect.width <= 0f || rect.height <= 0f) return@forEachIndexed
                    TreemapTile(item, rect)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TreemapTile(item: TreemapItem, rect: TreemapRect) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset(x = rect.x.dp, y = rect.y.dp)
            .size(width = rect.width.dp, height = rect.height.dp)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(item.color)
            .padding(6.dp)
    ) {
        if (rect.width >= MinTileWidthForLabel && rect.height >= MinTileHeightForLabel) {
            Column {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (rect.height >= MinTileHeightForValue) {
                    Text(
                        text = item.displayValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Squarified treemap layout (Bruls, Huizing, van Wijk): recursively lays out [values] as a row along the
 * shorter side of the remaining rectangle, extending the row while it keeps tile aspect ratios closer to
 * square, then starts a new row for what's left. Returns one rect per value, same order as input.
 */
private fun squarify(values: List<Float>, rect: TreemapRect): List<TreemapRect> {
    val total = values.sum()
    if (total <= 0f || rect.width <= 0f || rect.height <= 0f) return values.map { TreemapRect(rect.x, rect.y, 0f, 0f) }

    val order = values.indices.sortedByDescending { values[it] }
    val areas = values.map { it / total * rect.width * rect.height }
    val orderedAreas = order.map { areas[it] }

    val placedInOrder = MutableList(order.size) { TreemapRect(rect.x, rect.y, 0f, 0f) }
    layoutSquarified(orderedAreas, rect, placedInOrder)

    val result = MutableList(values.size) { TreemapRect(rect.x, rect.y, 0f, 0f) }
    order.forEachIndexed { position, originalIndex -> result[originalIndex] = placedInOrder[position] }
    return result
}

private fun layoutSquarified(areas: List<Float>, bounds: TreemapRect, out: MutableList<TreemapRect>) {
    var remainingBounds = bounds
    var startIndex = 0
    while (startIndex < areas.size) {
        val side = minOf(remainingBounds.width, remainingBounds.height)
        var rowEnd = startIndex + 1
        var rowWorst = worstRatio(areas.subList(startIndex, rowEnd), side)
        while (rowEnd < areas.size) {
            val extendedWorst = worstRatio(areas.subList(startIndex, rowEnd + 1), side)
            if (extendedWorst > rowWorst) break
            rowWorst = extendedWorst
            rowEnd++
        }
        remainingBounds = layoutRow(areas, startIndex, rowEnd, remainingBounds, out)
        startIndex = rowEnd
    }
}

private fun worstRatio(row: List<Float>, side: Float): Float {
    val sum = row.sum()
    if (sum <= 0f || side <= 0f) return Float.MAX_VALUE
    val maxArea = row.max()
    val minArea = row.min()
    val sideSq = side * side
    val sumSq = sum * sum
    return maxOf(sideSq * maxArea / sumSq, sumSq / (sideSq * minArea))
}

/** Lays out `areas[startIndex until rowEnd]` as a strip along the shorter side of [bounds], returns the leftover rect. */
private fun layoutRow(
    areas: List<Float>,
    startIndex: Int,
    rowEnd: Int,
    bounds: TreemapRect,
    out: MutableList<TreemapRect>
): TreemapRect {
    val rowSum = areas.subList(startIndex, rowEnd).sum()
    if (rowSum <= 0f) return bounds

    return if (bounds.width >= bounds.height) {
        // Wider than tall: lay the row as a vertical strip on the left, full height.
        val stripWidth = rowSum / bounds.height
        var y = bounds.y
        for (i in startIndex until rowEnd) {
            val tileHeight = areas[i] / stripWidth
            out[i] = TreemapRect(bounds.x, y, stripWidth, tileHeight)
            y += tileHeight
        }
        TreemapRect(bounds.x + stripWidth, bounds.y, bounds.width - stripWidth, bounds.height)
    } else {
        // Taller than wide: lay the row as a horizontal strip on top, full width.
        val stripHeight = rowSum / bounds.width
        var x = bounds.x
        for (i in startIndex until rowEnd) {
            val tileWidth = areas[i] / stripHeight
            out[i] = TreemapRect(x, bounds.y, tileWidth, stripHeight)
            x += tileWidth
        }
        TreemapRect(bounds.x, bounds.y + stripHeight, bounds.width, bounds.height - stripHeight)
    }
}
