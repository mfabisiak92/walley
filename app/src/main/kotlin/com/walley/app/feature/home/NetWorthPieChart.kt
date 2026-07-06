package com.walley.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.walley.app.core.format.formatMoney
import com.walley.app.domain.model.Currency
import java.math.RoundingMode

private val SliceColors = listOf(
    Color(0xFF00695C),
    Color(0xFFFFB74D),
    Color(0xFF1565C0),
    Color(0xFFC62828),
    Color(0xFF6A1B9A),
    Color(0xFF2E7D32)
)

@Composable
fun NetWorthPieChart(breakdown: List<NetWorthByCurrency>, baseCurrency: Currency) {
    if (breakdown.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Net worth by currency", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(modifier = Modifier.size(120.dp)) {
                    Canvas(modifier = Modifier.fillMaxWidth().size(120.dp)) {
                        var startAngle = -90f
                        breakdown.forEachIndexed { index, slice ->
                            val sweep = (slice.percent.toFloat() / 100f) * 360f
                            drawArc(
                                color = SliceColors[index % SliceColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                size = Size(size.width, size.height)
                            )
                            startAngle += sweep
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    breakdown.forEachIndexed { index, slice ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SliceColors[index % SliceColors.size])
                            )
                            Text(
                                text = "  ${slice.currency.name} · " +
                                    "${slice.percent.setScale(1, RoundingMode.HALF_UP)}% · " +
                                    formatMoney(slice.amountInBaseCurrency, baseCurrency),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
