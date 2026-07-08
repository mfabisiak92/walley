package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiabilityTest {

    private fun liability(originalAmount: String, currentBalance: String) = Liability(
        name = "Liability",
        currency = Currency.PLN,
        originalAmount = BigDecimal(originalAmount),
        currentBalance = BigDecimal(currentBalance),
        startDate = LocalDate.now()
    )

    @Test
    fun `paidOffAmount is original amount minus current balance`() {
        assertEquals(BigDecimal("300"), liability(originalAmount = "1000", currentBalance = "700").paidOffAmount)
    }

    @Test
    fun `paidOffPercent is the paid-off percentage`() {
        val result = liability(originalAmount = "1000", currentBalance = "700").paidOffPercent
        assertEquals(0, BigDecimal("30").compareTo(result))
    }

    @Test
    fun `paidOffPercent is null when the original amount was zero or negative`() {
        assertNull(liability(originalAmount = "0", currentBalance = "0").paidOffPercent)
    }

    @Test
    fun `paidOffPercent is clamped to 100 even if overpaid`() {
        val result = liability(originalAmount = "1000", currentBalance = "-500").paidOffPercent
        assertEquals(0, BigDecimal("100").compareTo(result))
    }

    @Test
    fun `paidOffPercent is clamped to 0 if the balance grew above the original amount`() {
        val result = liability(originalAmount = "1000", currentBalance = "1500").paidOffPercent
        assertEquals(0, BigDecimal.ZERO.compareTo(result))
    }
}
