package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AssetTest {

    private fun asset(purchaseValue: String, currentValue: String) = Asset(
        name = "Asset",
        currency = Currency.PLN,
        purchaseValue = BigDecimal(purchaseValue),
        currentValue = BigDecimal(currentValue),
        purchaseDate = LocalDate.now()
    )

    @Test
    fun `gain is current value minus purchase value`() {
        assertEquals(BigDecimal("200"), asset(purchaseValue = "800", currentValue = "1000").gain)
    }

    @Test
    fun `gain is negative on a loss`() {
        assertEquals(BigDecimal("-200"), asset(purchaseValue = "1000", currentValue = "800").gain)
    }

    @Test
    fun `gainPercent is the value change percentage`() {
        val result = asset(purchaseValue = "800", currentValue = "1000").gainPercent
        assertEquals(0, BigDecimal("25").compareTo(result))
    }

    @Test
    fun `gainPercent is null when purchase value was zero`() {
        assertNull(asset(purchaseValue = "0", currentValue = "1000").gainPercent)
    }
}
