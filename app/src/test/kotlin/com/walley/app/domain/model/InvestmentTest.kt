package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InvestmentTest {

    private fun investment(currentPrice: String) = Investment(
        name = "Investment",
        ticker = "TCK",
        category = InvestmentCategory.STOCK,
        currency = Currency.PLN,
        currentPrice = BigDecimal(currentPrice)
    )

    private fun buy(date: String, quantity: String, price: String, id: Long = 0, commission: String = "0") = InvestmentTransaction(
        id = id,
        type = InvestmentTransactionType.BUY,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        pricePerUnit = BigDecimal(price),
        commission = BigDecimal(commission)
    )

    private fun sell(date: String, quantity: String, price: String, id: Long = 0, commission: String = "0") = InvestmentTransaction(
        id = id,
        type = InvestmentTransactionType.SELL,
        date = LocalDate.parse(date),
        quantity = BigDecimal(quantity),
        pricePerUnit = BigDecimal(price),
        commission = BigDecimal(commission)
    )

    private fun account(
        uninvestedCash: String,
        commissionFlat: String = "0",
        commissionPercent: String = "0",
        id: Long = 0,
        taxRate: AccountTaxRate = AccountTaxRate.STANDARD_19,
        currency: Currency = Currency.PLN
    ) = Account(
        id = id,
        name = "Brokerage",
        type = AccountType.INVESTMENT,
        currency = currency,
        balance = BigDecimal(uninvestedCash),
        uninvestedCash = BigDecimal(uninvestedCash),
        commissionFlat = BigDecimal(commissionFlat),
        commissionPercent = BigDecimal(commissionPercent),
        taxRate = taxRate
    )

    @Test
    fun `single buy sets quantity, average cost, and cost basis`() {
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "10", "20")))
        assertEquals(BigDecimal("10"), data.quantity)
        assertEquals(0, BigDecimal("20").compareTo(data.averageCost))
        assertEquals(0, BigDecimal("200").compareTo(data.costBasis))
        assertEquals(0, BigDecimal("250").compareTo(data.currentValue))
    }

    @Test
    fun `two buys at different prices produce a weighted average cost`() {
        // 10 @ 20 then 10 @ 30 -> 20 units at avg cost 25
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(buy("2026-01-01", "10", "20"), buy("2026-02-01", "10", "30"))
        )
        assertEquals(BigDecimal("20"), data.quantity)
        assertEquals(0, BigDecimal("25").compareTo(data.averageCost))
        assertEquals(LocalDate.parse("2026-01-01"), data.firstPurchaseDate)
    }

    @Test
    fun `unrealizedGainLoss is current value minus cost basis`() {
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "10", "20")))
        assertEquals(0, BigDecimal("50").compareTo(data.unrealizedGainLoss))
    }

    @Test
    fun `unrealizedGainLoss is negative on a loss`() {
        val data = InvestmentWithTransactions(investment("15"), listOf(buy("2026-01-01", "10", "20")))
        assertEquals(0, BigDecimal("-50").compareTo(data.unrealizedGainLoss))
    }

    @Test
    fun `unrealizedGainLossPercent is the price change percentage`() {
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "10", "20")))
        assertEquals(0, BigDecimal("25").compareTo(data.unrealizedGainLossPercent))
    }

    @Test
    fun `unrealizedGainLossPercent is null when the average cost was zero`() {
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "10", "0")))
        assertNull(data.unrealizedGainLossPercent)
    }

    @Test
    fun `a partial sell reduces quantity but keeps the average cost of the remainder`() {
        // Buy 10 @ 20, sell 4 @ 30 -> 6 remaining at avg cost 20, realized gain (30-20)*4 = 40
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-03-01", "4", "30"))
        )
        assertEquals(BigDecimal("6"), data.quantity)
        assertEquals(0, BigDecimal("20").compareTo(data.averageCost))
        assertEquals(0, BigDecimal("40").compareTo(data.realizedGainLoss))
    }

    @Test
    fun `selling the entire position leaves zero quantity and full realized gain`() {
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-03-01", "10", "30"))
        )
        assertEquals(BigDecimal("0"), data.quantity)
        assertEquals(0, BigDecimal("100").compareTo(data.realizedGainLoss))
        assertEquals(BigDecimal.ZERO, data.currentValue)
    }

    @Test
    fun `transactions out of chronological input order are still aggregated correctly`() {
        // Listed newest-first (as the repository returns them), but math must follow actual dates.
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(sell("2026-03-01", "4", "30"), buy("2026-01-01", "10", "20"))
        )
        assertEquals(BigDecimal("6"), data.quantity)
        assertEquals(0, BigDecimal("40").compareTo(data.realizedGainLoss))
    }

    @Test
    fun `quantityAvailableOn reflects holdings as of that date, not the full history`() {
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(buy("2026-01-01", "10", "20"), buy("2026-03-01", "5", "20"))
        )
        assertEquals(BigDecimal("10"), data.quantityAvailableOn(LocalDate.parse("2026-02-01")))
        assertEquals(BigDecimal("15"), data.quantityAvailableOn(LocalDate.parse("2026-03-01")))
    }

    @Test
    fun `quantityAvailableOn excludes the transaction being edited`() {
        val sellTx = sell("2026-02-01", "3", "30", id = 99)
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(buy("2026-01-01", "10", "20"), sellTx)
        )
        // Without excluding itself, re-validating that same sell would count its own 3 units as already gone.
        assertEquals(BigDecimal("7"), data.quantityAvailableOn(LocalDate.parse("2026-02-01")))
        assertEquals(BigDecimal("10"), data.quantityAvailableOn(LocalDate.parse("2026-02-01"), excludingTransactionId = 99))
    }

    @Test
    fun `quantityAvailableOn never goes negative even if a prior sell already overshot`() {
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(buy("2026-01-01", "5", "20"), sell("2026-02-01", "5", "30"))
        )
        assertEquals(BigDecimal.ZERO, data.quantityAvailableOn(LocalDate.parse("2026-02-01")))
    }

    @Test
    fun `availableCashToBuy subtracts a buy's total cost from uninvested cash`() {
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "10", "20")))
        val cash = availableCashToBuy(account("1000"), listOf(data), LocalDate.parse("2026-02-01"))
        assertEquals(0, BigDecimal("800").compareTo(cash))
    }

    @Test
    fun `availableCashToBuy adds back proceeds from a sell`() {
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-02-01", "5", "20"))
        )
        // Spent 200 buying, got 100 back selling -> net 100 spent, 900 left of a 1000 pool.
        val cash = availableCashToBuy(account("1000"), listOf(data), LocalDate.parse("2026-03-01"))
        assertEquals(0, BigDecimal("900").compareTo(cash))
    }

    @Test
    fun `availableCashToBuy nets across every investment sharing the account's cash pool`() {
        val first = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "10", "20")))
        val second = InvestmentWithTransactions(investment("10"), listOf(buy("2026-01-01", "30", "10")))
        // 200 spent on the first, 300 on the second, out of a shared 1000 pool.
        val cash = availableCashToBuy(account("1000"), listOf(first, second), LocalDate.parse("2026-02-01"))
        assertEquals(0, BigDecimal("500").compareTo(cash))
    }

    @Test
    fun `availableCashToBuy ignores transactions dated after the given date`() {
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-05-01", "10", "20")))
        val cash = availableCashToBuy(account("1000"), listOf(data), LocalDate.parse("2026-02-01"))
        assertEquals(0, BigDecimal("1000").compareTo(cash))
    }

    @Test
    fun `availableCashToBuy excludes the transaction being edited`() {
        val buyTx = buy("2026-01-01", "10", "20", id = 42)
        val data = InvestmentWithTransactions(investment("25"), listOf(buyTx))
        val cash = availableCashToBuy(account("1000"), listOf(data), LocalDate.parse("2026-02-01"), excludingTransactionId = 42)
        assertEquals(0, BigDecimal("1000").compareTo(cash))
    }

    @Test
    fun `availableCashToBuy never goes negative when the account is already overspent`() {
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "100", "20")))
        val cash = availableCashToBuy(account("1000"), listOf(data), LocalDate.parse("2026-02-01"))
        assertEquals(0, BigDecimal.ZERO.compareTo(cash))
    }

    @Test
    fun `defaultCommission picks whichever of flat or percent is higher`() {
        val acc = account("1000", commissionFlat = "5", commissionPercent = "1")
        // 1% of 200 = 2, less than the 5 flat fee -> flat wins.
        assertEquals(0, BigDecimal("5.00").compareTo(acc.defaultCommission(BigDecimal("200"))))
        // 1% of 1000 = 10, more than the 5 flat fee -> percent wins.
        assertEquals(0, BigDecimal("10.00").compareTo(acc.defaultCommission(BigDecimal("1000"))))
    }

    @Test
    fun `a buy's commission is folded into average cost`() {
        // 10 units @ 20 = 200, plus 10 commission -> 210 for 10 units = 21 average cost.
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "10", "20", commission = "10")))
        assertEquals(0, BigDecimal("21").compareTo(data.averageCost))
        assertEquals(0, BigDecimal("10").compareTo(data.totalCommissionPaid))
    }

    @Test
    fun `a sell's commission reduces realized gain`() {
        // Buy 10 @ 20 (cost 20/unit). Sell 10 @ 30 with 20 commission -> net proceeds 280, i.e. 28/unit.
        // Realized gain = (28 - 20) * 10 = 80, instead of 100 without commission.
        val data = InvestmentWithTransactions(
            investment("25"),
            listOf(buy("2026-01-01", "10", "20"), sell("2026-02-01", "10", "30", commission = "20"))
        )
        assertEquals(0, BigDecimal("80").compareTo(data.realizedGainLoss))
    }

    @Test
    fun `availableCashToBuy nets out commission on top of trade value`() {
        // Buy 10 @ 20 = 200, plus 10 commission -> 210 spent from a 1000 pool.
        val data = InvestmentWithTransactions(investment("25"), listOf(buy("2026-01-01", "10", "20", commission = "10")))
        val cash = availableCashToBuy(account("1000"), listOf(data), LocalDate.parse("2026-02-01"))
        assertEquals(0, BigDecimal("790").compareTo(cash))
    }
}
