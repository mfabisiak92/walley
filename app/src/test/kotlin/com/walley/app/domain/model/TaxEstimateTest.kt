package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaxEstimateTest {

    private fun account(
        id: Long,
        taxRate: AccountTaxRate = AccountTaxRate.STANDARD_19,
        currency: Currency = Currency.PLN
    ) = Account(
        id = id,
        name = "Account $id",
        type = AccountType.INVESTMENT,
        currency = currency,
        balance = BigDecimal.ZERO,
        taxRate = taxRate
    )

    private fun investment(currency: Currency = Currency.PLN) = Investment(
        name = "Investment",
        ticker = "TCK",
        category = InvestmentCategory.STOCK,
        currency = currency,
        currentPrice = BigDecimal("10")
    )

    private fun buy(date: String, quantity: String, price: String) = InvestmentTransaction(
        type = InvestmentTransactionType.BUY,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        pricePerUnit = BigDecimal(price)
    )

    private fun sell(date: String, quantity: String, price: String) = InvestmentTransaction(
        type = InvestmentTransactionType.SELL,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        pricePerUnit = BigDecimal(price)
    )

    @Test
    fun `estimatedTaxForYear only counts sells in the given year`() {
        // Bought 10 @ 20 in 2025, sold 10 @ 30 in 2026 -> gain 100 realized in 2026, none in 2025.
        val data = InvestmentWithTransactions(
            investment(),
            listOf(buy("2025-01-01", "10", "20"), sell("2026-06-01", "10", "30"))
        )
        val acc = account(id = 1)
        assertNull(acc.estimatedTaxForYear(listOf(data), 2025))
        assertEquals(0, BigDecimal("19.00").compareTo(acc.estimatedTaxForYear(listOf(data), 2026)))
    }

    @Test
    fun `estimatedTaxForYear is null for a tax-free account`() {
        val data = InvestmentWithTransactions(
            investment(),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-06-01", "10", "30"))
        )
        val acc = account(id = 1, taxRate = AccountTaxRate.TAX_FREE)
        assertNull(acc.estimatedTaxForYear(listOf(data), 2026))
    }

    @Test
    fun `estimatedTaxForYear is null when the account has a net loss for the year`() {
        val data = InvestmentWithTransactions(
            investment(),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-06-01", "10", "15"))
        )
        val acc = account(id = 1)
        assertNull(acc.estimatedTaxForYear(listOf(data), 2026))
    }

    @Test
    fun `estimatePortfolioTax nets a loss in one account against a gain in another`() {
        val gainingAccount = account(id = 1)
        val losingAccount = account(id = 2)
        val gainData = InvestmentWithTransactions(
            investment(),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-06-01", "10", "30"))
        )
        val lossData = InvestmentWithTransactions(
            investment(),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-06-01", "10", "10"))
        )
        // Gain of 100, loss of 100 -> net zero, no tax owed.
        val estimate = estimatePortfolioTax(
            accounts = listOf(gainingAccount, losingAccount),
            investmentsByAccount = mapOf(1L to listOf(gainData), 2L to listOf(lossData)),
            year = 2026
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(estimate.netRealizedGainLoss))
        assertEquals(0, BigDecimal.ZERO.compareTo(estimate.taxOwed))
    }

    @Test
    fun `estimatePortfolioTax excludes tax-free accounts entirely`() {
        val taxFreeAccount = account(id = 1, taxRate = AccountTaxRate.TAX_FREE)
        val gainData = InvestmentWithTransactions(
            investment(),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-06-01", "10", "30"))
        )
        val estimate = estimatePortfolioTax(
            accounts = listOf(taxFreeAccount),
            investmentsByAccount = mapOf(1L to listOf(gainData)),
            year = 2026
        )
        assertEquals(0, BigDecimal.ZERO.compareTo(estimate.netRealizedGainLoss))
        assertEquals(0, BigDecimal.ZERO.compareTo(estimate.taxOwed))
    }

    @Test
    fun `estimatePortfolioTax converts each account's currency before netting`() {
        val plnAccount = account(id = 1, currency = Currency.PLN)
        val usdAccount = account(id = 2, currency = Currency.USD)
        val plnGain = InvestmentWithTransactions(
            investment(Currency.PLN),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-06-01", "10", "30"))
        )
        // A 100 USD loss, converted at 1 USD = 4 PLN -> 400 PLN loss, fully offsetting the 100 PLN gain... wait,
        // use a smaller loss so the net stays a gain to keep the assertion simple.
        val usdLoss = InvestmentWithTransactions(
            investment(Currency.USD),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-06-01", "10", "18"))
        )
        val estimate = estimatePortfolioTax(
            accounts = listOf(plnAccount, usdAccount),
            investmentsByAccount = mapOf(1L to listOf(plnGain), 2L to listOf(usdLoss)),
            year = 2026,
            convertToBase = { amount, currency -> if (currency == Currency.USD) amount * BigDecimal("4") else amount }
        )
        // PLN gain of 100, USD loss of 20 converted to 80 PLN -> net 20 PLN gain, tax = 20 * 0.19 = 3.80.
        assertEquals(0, BigDecimal("20").compareTo(estimate.netRealizedGainLoss))
        assertEquals(0, BigDecimal("3.80").compareTo(estimate.taxOwed))
    }

    @Test
    fun `estimatedTaxByYear computes each year with a sell independently`() {
        // 2025: sold at a gain (100). 2026: sold at a gain (50). Both years owe tax, computed separately.
        val data = InvestmentWithTransactions(
            investment(),
            listOf(
                buy("2025-01-01", "10", "20"),
                sell("2025-06-01", "10", "30"),
                buy("2026-01-01", "10", "20"),
                sell("2026-06-01", "10", "25")
            )
        )
        val byYear = estimatedTaxByYear(listOf(account(id = 1)), mapOf(1L to listOf(data)))
        assertEquals(0, BigDecimal("19.00").compareTo(byYear.getValue(2025)))
        assertEquals(0, BigDecimal("9.50").compareTo(byYear.getValue(2026)))
    }

    @Test
    fun `estimatedTaxByYear omits years with nothing owed`() {
        // 2025 has a sell but a net loss, so it's owed nothing and shouldn't appear at all.
        val data = InvestmentWithTransactions(
            investment(),
            listOf(buy("2025-01-01", "10", "20"), sell("2025-06-01", "10", "15"))
        )
        val byYear = estimatedTaxByYear(listOf(account(id = 1)), mapOf(1L to listOf(data)))
        assertEquals(emptyMap<Int, BigDecimal>(), byYear)
    }

    @Test
    fun `estimatedTaxByYear ignores years with no sells at all`() {
        // Only a buy in 2025, never sold -> no realized gain, no year should appear.
        val data = InvestmentWithTransactions(investment(), listOf(buy("2025-01-01", "10", "20")))
        val byYear = estimatedTaxByYear(listOf(account(id = 1)), mapOf(1L to listOf(data)))
        assertEquals(emptyMap<Int, BigDecimal>(), byYear)
    }
}
