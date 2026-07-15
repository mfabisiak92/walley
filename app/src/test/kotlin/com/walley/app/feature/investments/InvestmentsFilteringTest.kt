package com.walley.app.feature.investments

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentSortField
import com.walley.app.domain.model.InvestmentTransaction
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.InvestmentsFilterState
import com.walley.app.domain.model.InvestmentsSortState
import com.walley.app.domain.model.PositionStatusFilter
import com.walley.app.domain.model.SortDirection
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class InvestmentsFilteringTest {

    private val rates = ExchangeRates(
        base = Currency.PLN,
        rates = mapOf(Currency.EUR to BigDecimal("0.25")),
        date = "2026-01-01"
    )

    private fun position(
        id: Long,
        name: String,
        category: InvestmentCategory = InvestmentCategory.STOCK,
        currency: Currency = Currency.PLN,
        currentPrice: String = "10",
        accountId: Long? = 1,
        quantity: String = "5",
        buyPrice: String = "10"
    ): InvestmentWithTransactions {
        val investment = Investment(
            id = id,
            name = name,
            ticker = name.take(3).uppercase(),
            category = category,
            currency = currency,
            currentPrice = BigDecimal(currentPrice),
            accountId = accountId
        )
        val transactions = if (BigDecimal(quantity).signum() == 0) {
            emptyList()
        } else {
            listOf(
                InvestmentTransaction(
                    id = id,
                    investmentId = id,
                    type = InvestmentTransactionType.BUY,
                    date = LocalDate.of(2026, 1, 1),
                    quantity = BigDecimal(quantity),
                    pricePerUnit = BigDecimal(buyPrice)
                )
            )
        }
        return InvestmentWithTransactions(investment, transactions)
    }

    @Test
    fun `filterInvestments default status keeps only open positions`() {
        val positions = listOf(position(1, "Open"), position(2, "Closed", quantity = "0"))
        val result = filterInvestments(positions, InvestmentsFilterState())
        assertEquals(listOf("Open"), result.map { it.investment.name })
    }

    @Test
    fun `filterInvestments CLOSED status keeps only closed positions`() {
        val positions = listOf(position(1, "Open"), position(2, "Closed", quantity = "0"))
        val result = filterInvestments(positions, InvestmentsFilterState(status = PositionStatusFilter.CLOSED))
        assertEquals(listOf("Closed"), result.map { it.investment.name })
    }

    @Test
    fun `filterInvestments empty category set matches everything`() {
        val positions = listOf(position(1, "Stock", category = InvestmentCategory.STOCK), position(2, "Crypto", category = InvestmentCategory.CRYPTO))
        val result = filterInvestments(positions, InvestmentsFilterState())
        assertEquals(2, result.size)
    }

    @Test
    fun `filterInvestments non-empty category set only matches selected categories`() {
        val positions = listOf(position(1, "Stock", category = InvestmentCategory.STOCK), position(2, "Crypto", category = InvestmentCategory.CRYPTO))
        val result = filterInvestments(positions, InvestmentsFilterState(categories = setOf(InvestmentCategory.CRYPTO)))
        assertEquals(listOf("Crypto"), result.map { it.investment.name })
    }

    @Test
    fun `filterInvestments account filter matches only selected accounts`() {
        val positions = listOf(position(1, "AccountOne", accountId = 1), position(2, "AccountTwo", accountId = 2))
        val result = filterInvestments(positions, InvestmentsFilterState(accountIds = setOf(2L)))
        assertEquals(listOf("AccountTwo"), result.map { it.investment.name })
    }

    @Test
    fun `sortInvestments NAME ASC sorts case-insensitively`() {
        val positions = listOf(position(1, "banana"), position(2, "Apple"))
        val result = sortInvestments(positions, InvestmentsSortState(InvestmentSortField.NAME, SortDirection.ASC), Currency.PLN, null)
        assertEquals(listOf("Apple", "banana"), result.map { it.investment.name })
    }

    @Test
    fun `sortInvestments VALUE DESC converts to base currency before comparing`() {
        // 10 units @ 10 EUR = 100 EUR, at rate 0.25 (1 PLN = 0.25 EUR) -> 400 PLN, beating the 300 PLN position.
        val positions = listOf(
            position(1, "PLN position", currency = Currency.PLN, currentPrice = "30", quantity = "10"),
            position(2, "EUR position", currency = Currency.EUR, currentPrice = "10", quantity = "10")
        )
        val result = sortInvestments(positions, InvestmentsSortState(InvestmentSortField.VALUE, SortDirection.DESC), Currency.PLN, rates)
        assertEquals(listOf("EUR position", "PLN position"), result.map { it.investment.name })
    }

    @Test
    fun `sortInvestments GAIN_LOSS_PERCENT DESC ranks the best performer first`() {
        val positions = listOf(
            position(1, "Flat", currentPrice = "10", buyPrice = "10"),
            position(2, "Up 50pct", currentPrice = "15", buyPrice = "10"),
            position(3, "Down 50pct", currentPrice = "5", buyPrice = "10")
        )
        val result = sortInvestments(positions, InvestmentsSortState(InvestmentSortField.GAIN_LOSS_PERCENT, SortDirection.DESC), Currency.PLN, null)
        assertEquals(listOf("Up 50pct", "Flat", "Down 50pct"), result.map { it.investment.name })
    }

    @Test
    fun `sortInvestments DATE_ADDED NEWEST first is DESC by id`() {
        val positions = listOf(position(1, "First"), position(2, "Second"), position(3, "Third"))
        val result = sortInvestments(positions, InvestmentsSortState(InvestmentSortField.DATE_ADDED, SortDirection.DESC), Currency.PLN, null)
        assertEquals(listOf("Third", "Second", "First"), result.map { it.investment.name })
    }
}
