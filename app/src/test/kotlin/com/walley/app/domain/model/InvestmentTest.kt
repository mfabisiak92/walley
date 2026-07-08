package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InvestmentTest {

    private fun investment(quantity: String, price: String, currentPrice: String) = Investment(
        name = "Investment",
        ticker = "TCK",
        category = InvestmentCategory.STOCK,
        purchaseDate = LocalDate.now(),
        quantity = BigDecimal(quantity),
        currency = Currency.PLN,
        price = BigDecimal(price),
        currentPrice = BigDecimal(currentPrice)
    )

    @Test
    fun `costBasis and currentValue multiply quantity by price`() {
        val inv = investment(quantity = "10", price = "20", currentPrice = "25")
        assertEquals(BigDecimal("200"), inv.costBasis)
        assertEquals(BigDecimal("250"), inv.currentValue)
    }

    @Test
    fun `gainLoss is current value minus cost basis`() {
        val inv = investment(quantity = "10", price = "20", currentPrice = "25")
        assertEquals(BigDecimal("50"), inv.gainLoss)
    }

    @Test
    fun `gainLoss is negative on a loss`() {
        val inv = investment(quantity = "10", price = "20", currentPrice = "15")
        assertEquals(BigDecimal("-50"), inv.gainLoss)
    }

    @Test
    fun `gainLossPercent is the price change percentage`() {
        val inv = investment(quantity = "10", price = "20", currentPrice = "25")
        assertEquals(0, BigDecimal("25").compareTo(inv.gainLossPercent))
    }

    @Test
    fun `gainLossPercent is null when the purchase price was zero`() {
        val inv = investment(quantity = "10", price = "0", currentPrice = "25")
        assertNull(inv.gainLossPercent)
    }
}
