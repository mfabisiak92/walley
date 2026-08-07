package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DividendsTest {

    private val investment = Investment(
        id = 1,
        name = "PKNORLEN",
        ticker = "PLPKN0000018",
        category = InvestmentCategory.STOCK,
        currency = Currency.PLN,
        currentPrice = BigDecimal("60")
    )

    private fun operation(description: String, amount: String, accountId: Long = 1) = AccountOperation(
        accountId = accountId,
        date = LocalDate.parse("2026-01-01"),
        description = description,
        amount = BigDecimal(amount)
    )

    @Test
    fun `sums dividend payouts mentioning this investment, ignoring the withholding tax row`() {
        val operations = listOf(
            operation("Wypłata dywidendy PKNORLEN", "2000"),
            operation("Podatek od odsetek lub dywidendy PKNORLEN", "-380"),
            operation("Wypłata dywidendy PKNORLEN", "1500")
        )
        assertEquals(0, BigDecimal("3500").compareTo(investment.dividendsPaid(operations)))
    }

    @Test
    fun `ignores operations for a different security`() {
        val operations = listOf(
            operation("Wypłata dywidendy PZU", "1000"),
            operation("Przelew do DM BOŚ", "5000")
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(investment.dividendsPaid(operations)))
    }

    @Test
    fun `matches the English wording too`() {
        val operations = listOf(operation("Dividend payout PKNORLEN", "100"))
        assertEquals(0, BigDecimal("100").compareTo(investment.dividendsPaid(operations)))
    }

    @Test
    fun `a positive non-dividend operation mentioning the name is not counted`() {
        val operations = listOf(operation("Przelew do DM BOŚ dla PKNORLEN", "5000"))
        assertEquals(0, BigDecimal.ZERO.compareTo(investment.dividendsPaid(operations)))
    }

    @Test
    fun `net dividends subtract the withholding tax that gross dividends exclude`() {
        val operations = listOf(
            operation("Wypłata dywidendy PKNORLEN", "2000"),
            operation("Podatek od odsetek lub dywidendy PKNORLEN", "-380")
        )
        assertEquals(0, BigDecimal("2000").compareTo(investment.dividendsPaid(operations)))
        assertEquals(0, BigDecimal("1620").compareTo(investment.netDividendsPaid(operations)))
    }

    @Test
    fun `net dividends equal gross when no matching tax row exists`() {
        val operations = listOf(operation("Wypłata dywidendy PKNORLEN", "2000"))
        assertEquals(0, investment.dividendsPaid(operations).compareTo(investment.netDividendsPaid(operations)))
    }
}
