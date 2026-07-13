package com.walley.app.feature.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

data class ComparisonValue(val current: BigDecimal?, val previous: BigDecimal?, val changePercent: BigDecimal?)

/** The element of [points] whose year/month (via [yearMonthOf]) exactly matches [target]; null if there's a gap. */
fun <T> findByYearMonth(points: List<T>, target: YearMonth, yearMonthOf: (T) -> YearMonth): T? =
    points.firstOrNull { yearMonthOf(it) == target }

/** `(current - previous) / |previous| * 100`; null if either side is missing or [previous] is zero. */
fun compare(current: BigDecimal?, previous: BigDecimal?): ComparisonValue {
    val changePercent = if (current == null || previous == null || previous.signum() == 0) {
        null
    } else {
        (current - previous).divide(previous.abs(), 4, RoundingMode.HALF_UP) * BigDecimal(100)
    }
    return ComparisonValue(current, previous, changePercent)
}

data class GrowthRatePoint(val yearMonth: YearMonth, val label: String, val momPercent: BigDecimal?, val yoyPercent: BigDecimal?)

/**
 * One growth-rate point per snapshot, chronological. [GrowthRatePoint.momPercent] compares to the
 * immediately preceding snapshot in the list — which may not be exactly one calendar month prior if
 * there's a gap, but is still the best available "prior" point, consistent with how the absolute net
 * worth chart already treats adjacency. [GrowthRatePoint.yoyPercent] instead requires an *exact*
 * 12-calendar-month-earlier match via [findByYearMonth]; null when that month has no snapshot.
 */
fun netWorthGrowthRates(snapshots: List<SnapshotPoint>): List<GrowthRatePoint> =
    snapshots.mapIndexed { index, point ->
        val previousAdjacent = snapshots.getOrNull(index - 1)?.netWorth
        val previousYear = findByYearMonth(snapshots, point.yearMonth.minusYears(1)) { it.yearMonth }?.netWorth
        GrowthRatePoint(
            yearMonth = point.yearMonth,
            label = point.label,
            momPercent = compare(point.netWorth, previousAdjacent).changePercent,
            yoyPercent = compare(point.netWorth, previousYear).changePercent
        )
    }
