package com.walley.app.feature.analytics

import com.walley.app.domain.model.AccountOperation
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentPricePoint
import com.walley.app.domain.model.InvestmentTransaction
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentYearlyGrowthTest {

    private val rates = ExchangeRates(
        base = Currency.PLN,
        rates = mapOf(Currency.EUR to BigDecimal("0.25")),
        date = "2026-01-01"
    )

    private fun position(currency: Currency, currentPrice: String, transactions: List<InvestmentTransaction>, id: Long = 1, accountId: Long? = null) =
        InvestmentWithTransactions(
            Investment(
                id = id,
                name = "Position $id",
                ticker = "T$id",
                category = InvestmentCategory.STOCK,
                currency = currency,
                currentPrice = BigDecimal(currentPrice),
                accountId = accountId
            ),
            transactions
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

    private fun pricePoint(date: String, price: String) = InvestmentPricePoint(LocalDate.parse(date), BigDecimal(price))

    @Test
    fun `growth is computed from true year-start and year-end value, not just contributions`() {
        // Bought 10 units @ 20 on 2024-01-01. History has a close at 2024-12-31 = 22; today (2025-12-31)
        // uses the live currentPrice (25) directly rather than a history lookup.
        val data = position(Currency.PLN, currentPrice = "25", transactions = listOf(buy("2024-01-01", "10", "20")))
        val history = mapOf(1L to listOf(pricePoint("2024-12-31", "22")))
        val today = LocalDate.parse("2025-12-31")

        val points = computeYearlyGrowth(listOf(data), history, emptyList(), emptyMap(), emptyMap(), Currency.PLN, rates, today)

        val year2024 = points.first { it.year == 2024 }
        assertEquals(0, BigDecimal.ZERO.compareTo(year2024.startValue)) // nothing held before the buy
        assertEquals(0, BigDecimal("220").compareTo(year2024.endValue)) // 10 * 22
        assertEquals(0, BigDecimal("200").compareTo(year2024.invested)) // 10 * 20
        assertEquals(0, BigDecimal("20").compareTo(year2024.growth)) // 220 - 0 - 200
        assertFalse(year2024.isEstimated)

        val year2025 = points.first { it.year == 2025 }
        assertEquals(0, BigDecimal("220").compareTo(year2025.startValue))
        assertEquals(0, BigDecimal("250").compareTo(year2025.endValue)) // 10 * currentPrice(25), today is year-end
        assertEquals(0, BigDecimal.ZERO.compareTo(year2025.invested))
        assertEquals(0, BigDecimal("30").compareTo(year2025.growth)) // 250 - 220
        assertFalse(year2025.isEstimated)
    }

    @Test
    fun `a holding bought mid-year starts that year's value at zero`() {
        val data = position(Currency.PLN, currentPrice = "15", transactions = listOf(buy("2026-06-01", "10", "10")))
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(listOf(data), emptyMap(), emptyList(), emptyMap(), emptyMap(), Currency.PLN, rates, today)

        val year2026 = points.single { it.year == 2026 }
        assertEquals(0, BigDecimal.ZERO.compareTo(year2026.startValue))
        assertEquals(0, BigDecimal("150").compareTo(year2026.endValue)) // 10 * currentPrice(15)
        assertEquals(0, BigDecimal("100").compareTo(year2026.invested)) // 10 * 10
        assertEquals(0, BigDecimal("50").compareTo(year2026.growth))
    }

    @Test
    fun `a past year with no price coverage falls back to the purchase-year approximation and is flagged estimated`() {
        // Bought in 2022, with no price history at all and "today" now two years later — 2022's
        // year-end is fully in the past, so it can't fall back to today's live currentPrice either.
        val data = position(Currency.PLN, currentPrice = "30", transactions = listOf(buy("2022-01-01", "10", "20")))
        val today = LocalDate.parse("2024-01-01")

        val points = computeYearlyGrowth(listOf(data), emptyMap(), emptyList(), emptyMap(), emptyMap(), Currency.PLN, rates, today)

        val year2022 = points.first { it.year == 2022 }
        assertTrue(year2022.isEstimated)
        // Falls back to unrealized gain attributed to the purchase year: 10 * (30 - 20) = 100.
        assertEquals(0, BigDecimal("100").compareTo(year2022.growth))
        assertEquals(0, BigDecimal("100").compareTo(year2022.byHolding.single().growth))
    }

    @Test
    fun `foreign-currency holdings are converted to the base currency`() {
        val data = position(Currency.EUR, currentPrice = "20", transactions = listOf(buy("2026-01-01", "10", "10")))
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(listOf(data), emptyMap(), emptyList(), emptyMap(), emptyMap(), Currency.PLN, rates, today)

        val year2026 = points.single { it.year == 2026 }
        // endValue = 10 * 20 = 200 EUR -> 200 / 0.25 = 800 PLN; invested = 100 EUR -> 400 PLN.
        assertEquals(0, BigDecimal("800").compareTo(year2026.endValue))
        assertEquals(0, BigDecimal("400").compareTo(year2026.invested))
        assertEquals(0, BigDecimal("400").compareTo(year2026.growth))
    }

    @Test
    fun `a paper gain on a taxable account is shown net of the account's tax rate`() {
        // Bought 10 @ 10, now worth 20 -> a 100 gross paper gain, never sold.
        val data = position(Currency.PLN, currentPrice = "20", transactions = listOf(buy("2026-01-01", "10", "10")), accountId = 1)
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            listOf(data),
            emptyMap(),
            emptyList(),
            mapOf(1L to Currency.PLN),
            mapOf(1L to BigDecimal("0.19")),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        // 100 gross, minus 19% -> 81 net, as if sold at year-end.
        assertEquals(0, BigDecimal("81").compareTo(year2026.growth))
        assertEquals(0, BigDecimal("81").compareTo(year2026.byHolding.single().growth))
    }

    @Test
    fun `a paper gain on a tax-free account is not reduced`() {
        val data = position(Currency.PLN, currentPrice = "20", transactions = listOf(buy("2026-01-01", "10", "10")), accountId = 1)
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            listOf(data),
            emptyMap(),
            emptyList(),
            mapOf(1L to Currency.PLN),
            mapOf(1L to BigDecimal.ZERO),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        assertEquals(0, BigDecimal("100").compareTo(year2026.growth))
    }

    @Test
    fun `an unlinked investment is not taxed even if its old account id would be taxable`() {
        // accountId is deliberately left null here — unlike the other tax tests — so this proves it's
        // the missing link, not just missing data, that avoids the tax: account 1 IS taxable below.
        val data = position(Currency.PLN, currentPrice = "20", transactions = listOf(buy("2026-01-01", "10", "10")))
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            listOf(data),
            emptyMap(),
            emptyList(),
            mapOf(1L to Currency.PLN),
            mapOf(1L to BigDecimal("0.19")),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        assertEquals(0, BigDecimal("100").compareTo(year2026.growth))
    }

    @Test
    fun `a loss on a taxable account is not reduced further`() {
        // Bought 10 @ 20, now worth 10 -> a 100 loss, never sold.
        val data = position(Currency.PLN, currentPrice = "10", transactions = listOf(buy("2026-01-01", "10", "20")), accountId = 1)
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            listOf(data),
            emptyMap(),
            emptyList(),
            mapOf(1L to Currency.PLN),
            mapOf(1L to BigDecimal("0.19")),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        assertEquals(0, BigDecimal("-100").compareTo(year2026.growth))
    }

    @Test
    fun `a deposit that hasn't been invested yet shows as deposited but doesn't drag growth down`() {
        // Bought 10 @ 10 (fully using up the 100 already in the account before this deposit), then a
        // fresh 500 lands in the account but is never put into a position — it just sits as cash.
        // currentPrice is unchanged from the buy price, so the position itself hasn't moved at all.
        val data = position(Currency.PLN, currentPrice = "10", transactions = listOf(buy("2026-01-01", "10", "10")))
        val deposit = AccountOperation(accountId = 1, date = LocalDate.parse("2026-06-01"), description = "Deposit", amount = BigDecimal("500"))
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            listOf(data),
            emptyMap(),
            listOf(deposit),
            mapOf(1L to Currency.PLN),
            emptyMap(),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        assertEquals(0, BigDecimal("500").compareTo(year2026.deposited))
        assertEquals(0, BigDecimal("100").compareTo(year2026.invested))
        // Uninvested cash was never part of startValue/endValue, so it must not depress growth: the
        // position's own value hasn't moved (bought and still priced at 10), so growth is exactly zero.
        assertEquals(0, BigDecimal.ZERO.compareTo(year2026.growth))
    }

    @Test
    fun `dividends add to growth instead of being counted as a deposit`() {
        val data = position(Currency.PLN, currentPrice = "10", transactions = listOf(buy("2026-01-01", "10", "10")))
        val dividend = AccountOperation(accountId = 1, date = LocalDate.parse("2026-06-01"), description = "Wypłata dywidendy Position 1", amount = BigDecimal("50"))
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            listOf(data),
            emptyMap(),
            listOf(dividend),
            mapOf(1L to Currency.PLN),
            emptyMap(),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        assertEquals(0, BigDecimal.ZERO.compareTo(year2026.deposited))
        assertEquals(0, BigDecimal("50").compareTo(year2026.investmentIncome))
        // Position value itself didn't move, so all of the year's growth comes from the dividend.
        assertEquals(0, BigDecimal("50").compareTo(year2026.growth))
    }

    @Test
    fun `withholding tax on a dividend nets against the payout in investment income`() {
        val data = position(Currency.PLN, currentPrice = "10", transactions = listOf(buy("2026-01-01", "10", "10")))
        val payout = AccountOperation(accountId = 1, date = LocalDate.parse("2026-06-01"), description = "Wypłata dywidendy Position 1", amount = BigDecimal("50"))
        val tax = AccountOperation(accountId = 1, date = LocalDate.parse("2026-06-01"), description = "Podatek od odsetek lub dywidendy Position 1", amount = BigDecimal("-9.50"))
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            listOf(data),
            emptyMap(),
            listOf(payout, tax),
            mapOf(1L to Currency.PLN),
            emptyMap(),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        assertEquals(0, BigDecimal("40.50").compareTo(year2026.investmentIncome))
        assertEquals(0, BigDecimal.ZERO.compareTo(year2026.deposited))
    }

    @Test
    fun `deposited is net of withdrawals within the same year`() {
        val deposit = AccountOperation(accountId = 1, date = LocalDate.parse("2026-03-01"), description = "Deposit", amount = BigDecimal("1000"))
        val withdrawal = AccountOperation(accountId = 1, date = LocalDate.parse("2026-09-01"), description = "Withdrawal", amount = BigDecimal("-200"))
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            emptyList(),
            emptyMap(),
            listOf(deposit, withdrawal),
            mapOf(1L to Currency.PLN),
            emptyMap(),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        assertEquals(0, BigDecimal("800").compareTo(year2026.deposited))
    }

    @Test
    fun `a transfer between the user's own accounts is excluded from deposited, not just netted`() {
        val genuineDeposit = AccountOperation(accountId = 1, date = LocalDate.parse("2026-02-01"), description = "Przelew do DM BOŚ", amount = BigDecimal("1000"))
        val transferOut = AccountOperation(accountId = 1, date = LocalDate.parse("2026-08-14"), description = "Przelew wewnętrzny z rachunku kasowego", amount = BigDecimal("-500"))
        val transferIn = AccountOperation(accountId = 2, date = LocalDate.parse("2026-08-14"), description = "Przelew wewnętrzny z rachunku kasowego", amount = BigDecimal("500"))
        val xtbTransfer = AccountOperation(accountId = 1, date = LocalDate.parse("2026-06-12"), description = "Transfer — Transfer from 51296707 to 52725876", amount = BigDecimal("-300"))
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(
            emptyList(),
            emptyMap(),
            listOf(genuineDeposit, transferOut, transferIn, xtbTransfer),
            mapOf(1L to Currency.PLN, 2L to Currency.PLN),
            emptyMap(),
            Currency.PLN,
            rates,
            today
        )

        val year2026 = points.single { it.year == 2026 }
        // Only the genuine external deposit counts; both transfer legs (and the XTB-worded one) are excluded entirely.
        assertEquals(0, BigDecimal("1000").compareTo(year2026.deposited))
    }

    @Test
    fun `invested is the gross buy total, not netted against sells in the same year`() {
        val data = position(
            Currency.PLN,
            currentPrice = "20",
            transactions = listOf(buy("2026-01-01", "10", "10"), sell("2026-06-01", "4", "15"))
        )
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(listOf(data), emptyMap(), emptyList(), emptyMap(), emptyMap(), Currency.PLN, rates, today)

        val year2026 = points.single { it.year == 2026 }
        // Bought 100 worth; the 60 from selling 4 units isn't subtracted from "invested".
        assertEquals(0, BigDecimal("100").compareTo(year2026.invested))
    }

    @Test
    fun `a holding bought in a later year does not appear in an earlier year's breakdown`() {
        val early = position(Currency.PLN, currentPrice = "20", transactions = listOf(buy("2023-01-01", "10", "10")), id = 1)
        val late = position(Currency.PLN, currentPrice = "20", transactions = listOf(buy("2026-01-01", "10", "10")), id = 2)
        val today = LocalDate.parse("2026-12-31")

        val points = computeYearlyGrowth(listOf(early, late), emptyMap(), emptyList(), emptyMap(), emptyMap(), Currency.PLN, rates, today)

        val year2023 = points.first { it.year == 2023 }
        assertEquals(listOf(1L), year2023.byHolding.map { it.investmentId })

        val year2026 = points.first { it.year == 2026 }
        assertEquals(setOf(1L, 2L), year2026.byHolding.map { it.investmentId }.toSet())
    }

    @Test
    fun `a fully sold holding stops appearing once closed out, but shows in the year it closed`() {
        val data = position(
            Currency.PLN,
            currentPrice = "20",
            transactions = listOf(buy("2023-01-01", "10", "10"), sell("2024-06-01", "10", "15")),
            id = 1
        )
        val today = LocalDate.parse("2025-06-01")

        val points = computeYearlyGrowth(listOf(data), emptyMap(), emptyList(), emptyMap(), emptyMap(), Currency.PLN, rates, today)

        val year2024 = points.first { it.year == 2024 }
        assertEquals(listOf(1L), year2024.byHolding.map { it.investmentId }) // the year it was sold

        val year2025 = points.first { it.year == 2025 }
        assertTrue(year2025.byHolding.isEmpty()) // fully closed out with nothing happening since
    }

    @Test
    fun `returns an empty list when there is no transaction or deposit history`() {
        assertEquals(emptyList<InvestmentYearGrowthPoint>(), computeYearlyGrowth(emptyList(), emptyMap(), emptyList(), emptyMap(), emptyMap(), Currency.PLN, rates))
    }
}
