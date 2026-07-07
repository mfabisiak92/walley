package com.walley.app.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Red at 0% paid, through amber, to green at 100% paid. */
fun paidProgressColor(progress: Float): Color {
    val red = PieChartColors[3]
    val amber = PieChartColors[1]
    val green = PieChartColors[5]
    return when {
        progress >= 1f -> green
        progress <= 0.5f -> lerp(red, amber, progress / 0.5f)
        else -> lerp(amber, green, (progress - 0.5f) / 0.5f)
    }
}
