package com.walley.app.feature.analytics

import java.math.BigDecimal
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodComparisonTest {

    private fun snapshotPoint(year: Int, month: Int, netWorth: String) = SnapshotPoint(
        yearMonth = YearMonth.of(year, month),
        label = "$month/$year",
        cashAndChecking = BigDecimal.ZERO,
        savings = BigDecimal.ZERO,
        investments = BigDecimal.ZERO,
        netWorth = BigDecimal(netWorth),
        salaryIncome = BigDecimal.ZERO,
        dividendsIncome = BigDecimal.ZERO,
        interestIncome = BigDecimal.ZERO,
        otherIncome = BigDecimal.ZERO,
        investmentGrowth = null
    )

    @Test
    fun `findByYearMonth returns the point with exact matching yearMonth`() {
        val points = listOf(1 to YearMonth.of(2026, 1), 2 to YearMonth.of(2026, 3))
        val found = findByYearMonth(points, YearMonth.of(2026, 3)) { it.second }
        assertEquals(2, found?.first)
    }

    @Test
    fun `findByYearMonth returns null when no point matches`() {
        val points = listOf(1 to YearMonth.of(2026, 1))
        assertNull(findByYearMonth(points, YearMonth.of(2026, 2)) { it.second })
    }

    @Test
    fun `compare computes a positive changePercent for growth`() {
        val result = compare(BigDecimal("110"), BigDecimal("100"))
        assertEquals(0, BigDecimal("10").compareTo(result.changePercent))
    }

    @Test
    fun `compare computes a negative changePercent for decline`() {
        val result = compare(BigDecimal("90"), BigDecimal("100"))
        assertEquals(0, BigDecimal("-10").compareTo(result.changePercent))
    }

    @Test
    fun `compare returns null changePercent when previous is zero`() {
        assertNull(compare(BigDecimal("100"), BigDecimal.ZERO).changePercent)
    }

    @Test
    fun `compare returns null changePercent when either side is null`() {
        assertNull(compare(null, BigDecimal("100")).changePercent)
        assertNull(compare(BigDecimal("100"), null).changePercent)
    }

    @Test
    fun `compare returns 0 changePercent when current equals previous`() {
        assertEquals(0, BigDecimal.ZERO.compareTo(compare(BigDecimal("50"), BigDecimal("50")).changePercent))
    }

    @Test
    fun `netWorthGrowthRates computes MoM against the immediately preceding snapshot`() {
        val snapshots = listOf(snapshotPoint(2026, 1, "1000"), snapshotPoint(2026, 2, "1100"))
        val growth = netWorthGrowthRates(snapshots)
        assertEquals(0, BigDecimal("10").compareTo(growth[1].momPercent))
    }

    @Test
    fun `netWorthGrowthRates leaves momPercent null for the very first snapshot`() {
        val snapshots = listOf(snapshotPoint(2026, 1, "1000"))
        assertNull(netWorthGrowthRates(snapshots)[0].momPercent)
    }

    @Test
    fun `netWorthGrowthRates computes YoY only on an exact 12-month match`() {
        val snapshots = listOf(snapshotPoint(2025, 6, "1000"), snapshotPoint(2026, 6, "1200"))
        val growth = netWorthGrowthRates(snapshots)
        assertEquals(0, BigDecimal("20").compareTo(growth[1].yoyPercent))
    }

    @Test
    fun `netWorthGrowthRates leaves yoyPercent null when there is a gap at 12 months earlier`() {
        val snapshots = listOf(snapshotPoint(2025, 5, "1000"), snapshotPoint(2026, 6, "1200"))
        val growth = netWorthGrowthRates(snapshots)
        assertNull(growth[1].yoyPercent)
    }
}
