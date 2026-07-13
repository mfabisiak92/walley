package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentPerformanceTest {

    private val investment = Investment(
        id = 1,
        name = "Test Co",
        ticker = "TST",
        category = InvestmentCategory.STOCK,
        currency = Currency.USD,
        currentPrice = BigDecimal("12")
    )

    private fun buy(date: String, quantity: String, price: String, commission: String = "0") = InvestmentTransaction(
        investmentId = 1,
        type = InvestmentTransactionType.BUY,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        pricePerUnit = BigDecimal(price),
        commission = BigDecimal(commission)
    )

    private fun sell(date: String, quantity: String, price: String, commission: String = "0") = InvestmentTransaction(
        investmentId = 1,
        type = InvestmentTransactionType.SELL,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        pricePerUnit = BigDecimal(price),
        commission = BigDecimal(commission)
    )

    @Test
    fun `xirr returns null when the only two flows share the same date`() {
        // One BUY, still fully open, asOf the same day -> the synthetic inflow lands on the buy's
        // own date too, so there's no time span to solve a rate over.
        val position = InvestmentWithTransactions(investment, listOf(buy("2026-01-01", "10", "10")))
        assertNull(position.xirr(asOf = LocalDate.parse("2026-01-01")))
    }

    @Test
    fun `xirr on an all-BUY open position uses currentValue as a synthetic final inflow`() {
        // Buy 100 @ $10 (=$1000) on 2024-01-01; current price $12 -> currentValue = $1200 as of asOf.
        val position = InvestmentWithTransactions(investment, listOf(buy("2024-01-01", "100", "10")))
        val result = position.xirr(asOf = LocalDate.parse("2025-01-01"))
        assertTrue(result != null)
        // ~20% annualized (day-count over a leap year nudges it slightly off an exact 20%).
        assertEquals(20.0, result!!.toDouble(), 2.0)
    }

    @Test
    fun `xirr on a fully realized position needs no synthetic inflow`() {
        // Buy 100 @ $10 on day 0, sell all 100 @ $12 a year later.
        val position = InvestmentWithTransactions(
            investment,
            listOf(buy("2024-01-01", "100", "10"), sell("2025-01-01", "100", "12"))
        )
        val result = position.xirr(asOf = LocalDate.parse("2026-01-01"))
        assertTrue(result != null)
        assertEquals(20.0, result!!.toDouble(), 2.0)
    }

    @Test
    fun `xirr on a position mixing realized and unrealized flows converges`() {
        val position = InvestmentWithTransactions(
            investment,
            listOf(
                buy("2024-01-01", "100", "10"),
                sell("2024-07-01", "40", "11"),
                buy("2024-09-01", "20", "11")
            )
        )
        val result = position.xirr(asOf = LocalDate.parse("2025-01-01"))
        assertTrue(result != null)
    }

    @Test
    fun `xirr returns null when there are fewer than 2 cash flows`() {
        val position = InvestmentWithTransactions(investment, emptyList())
        assertNull(position.xirr())
    }

    @Test
    fun `xirr returns null for a holding period under the minimum annualization window`() {
        // Bought 5 days ago and up slightly — annualizing a few days' move would blow up into an
        // absurd percentage rather than a meaningful rate, so this must come back null, not a number.
        val position = InvestmentWithTransactions(investment, listOf(buy("2026-01-01", "100", "10")))
        assertNull(position.xirr(asOf = LocalDate.parse("2026-01-06")))
    }

    @Test
    fun `cagr returns null when there is no purchase date`() {
        val position = InvestmentWithTransactions(investment, emptyList())
        assertNull(position.cagr())
    }

    @Test
    fun `cagr returns null when begin value is zero`() {
        val position = InvestmentWithTransactions(investment, listOf(buy("2024-01-01", "0", "10")))
        assertNull(position.cagr(asOf = LocalDate.parse("2025-01-01")))
    }

    @Test
    fun `cagr returns null for a holding period under the minimum annualization window`() {
        val position = InvestmentWithTransactions(investment, listOf(buy("2026-01-01", "100", "10")))
        assertNull(position.cagr(asOf = LocalDate.parse("2026-01-06")))
    }

    @Test
    fun `cagr computes the annualized return for a simple double-in-2-years position`() {
        // Buy 100 @ $10 (=$1000); 2 years later price doubled to $20 -> currentValue = $2000.
        val doubled = investment.copy(currentPrice = BigDecimal("20"))
        val position = InvestmentWithTransactions(doubled, listOf(buy("2024-01-01", "100", "10")))
        val result = position.cagr(asOf = LocalDate.parse("2026-01-01"))
        assertTrue(result != null)
        // sqrt(2) - 1 ~= 41.4%
        assertEquals(41.4, result!!.toDouble(), 2.0)
    }

    @Test
    fun `cagr accounts for sell proceeds already returned, not just current value`() {
        // Buy 100 @ $10 (=$1000), sell half @ $15 a year in ($750 back), current price $12 for the rest.
        val position = InvestmentWithTransactions(
            investment,
            listOf(buy("2024-01-01", "100", "10"), sell("2025-01-01", "50", "15"))
        )
        val result = position.cagr(asOf = LocalDate.parse("2026-01-01"))
        assertTrue(result != null)
        // endValue = currentValue(50 @ $12 = $600) + sellProceeds($750) = $1350 vs beginValue $1000 over 2 years.
        assertEquals(16.2, result!!.toDouble(), 2.0)
    }
}
