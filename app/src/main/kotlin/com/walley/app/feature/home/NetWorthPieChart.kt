package com.walley.app.feature.home

import androidx.compose.runtime.Composable
import com.walley.app.core.format.formatMoney
import com.walley.app.core.ui.PieChartCard
import com.walley.app.core.ui.PieChartColors
import com.walley.app.core.ui.PieSlice
import com.walley.app.domain.model.Currency
import java.math.RoundingMode

@Composable
fun NetWorthPieChart(breakdown: List<NetWorthByCurrency>, baseCurrency: Currency) {
    val slices = breakdown.mapIndexed { index, slice ->
        PieSlice(
            label = "${slice.currency.name} · ${slice.percent.setScale(1, RoundingMode.HALF_UP)}% · " +
                formatMoney(slice.amountInBaseCurrency, baseCurrency),
            percent = slice.percent.toFloat(),
            color = PieChartColors[index % PieChartColors.size]
        )
    }
    PieChartCard(title = "Net worth by currency", slices = slices)
}
