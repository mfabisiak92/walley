package com.walley.app.feature.analytics

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransaction
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioGainsTest {

    private val rates = ExchangeRates(
        base = Currency.PLN,
        rates = mapOf(Currency.USD to BigDecimal("0.27")),
        date = "2026-01-01"
    )

    private fun position(currency: Currency, currentPrice: String, transactions: List<InvestmentTransaction>) =
        InvestmentWithTransactions(
            Investment(name = "T", ticker = "T", category = InvestmentCategory.STOCK, currency = currency, currentPrice = BigDecimal(currentPrice)),
            transactions
        )

    private fun buy(quantity: String, price: String) = InvestmentTransaction(
        type = InvestmentTransactionType.BUY,
        date = LocalDate.parse("2026-01-01"),
        quantity = BigDecimal(quantity),
        pricePerUnit = BigDecimal(price)
    )

    private fun sell(quantity: String, price: String) = InvestmentTransaction(
        type = InvestmentTransactionType.SELL,
        date = LocalDate.parse("2026-02-01"),
        quantity = BigDecimal(quantity),
        pricePerUnit = BigDecimal(price)
    )

    @Test
    fun `portfolioGainsSummary sums realized and unrealized gains across investments`() {
        val positions = listOf(
            position(Currency.PLN, "15", listOf(buy("10", "10"))), // unrealized: (15-10)*10 = 50
            position(Currency.PLN, "10", listOf(buy("10", "10"), sell("5", "12"))) // realized: (12-10)*5=10; unrealized: (10-10)*5=0
        )
        val summary = portfolioGainsSummary(positions, Currency.PLN, rates)
        assertEquals(0, BigDecimal("10").compareTo(summary?.realizedGainLoss))
        assertEquals(0, BigDecimal("50").compareTo(summary?.unrealizedGainLoss))
    }

    @Test
    fun `portfolioGainsSummary converts across currencies`() {
        val positions = listOf(position(Currency.USD, "20", listOf(buy("10", "10"))))
        // unrealized in USD = (20-10)*10 = 100 USD -> 100 / 0.27 = 370.37 PLN
        val summary = portfolioGainsSummary(positions, Currency.PLN, rates)
        assertEquals(0, BigDecimal("370.370370").compareTo(summary?.unrealizedGainLoss))
    }

    @Test
    fun `portfolioGainsSummary returns null when a rate is missing`() {
        val positions = listOf(position(Currency.GBP, "20", listOf(buy("10", "10"))))
        assertNull(portfolioGainsSummary(positions, Currency.PLN, rates))
    }

    @Test
    fun `portfolioGainsSummary returns zero totals for an empty portfolio`() {
        val summary = portfolioGainsSummary(emptyList(), Currency.PLN, rates)
        assertEquals(BigDecimal.ZERO, summary?.realizedGainLoss)
        assertEquals(BigDecimal.ZERO, summary?.unrealizedGainLoss)
    }
}
