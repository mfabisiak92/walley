package com.walley.app.feature.investments

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransaction
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceUpdateReviewTest {

    // Mirrors AccountRepositoryImpl.observeAccounts(): for a real INVESTMENT account, balance is
    // always uninvestedCash plus the current market value of its linked investments — computeAccountBalanceChanges
    // relies on that invariant already holding for the "before" snapshot it's given.
    private fun account(id: Long, uninvestedCash: String = "0", balance: String = uninvestedCash) = Account(
        id = id,
        name = "Brokerage $id",
        type = AccountType.INVESTMENT,
        currency = Currency.PLN,
        balance = BigDecimal(balance),
        uninvestedCash = BigDecimal(uninvestedCash),
        taxRate = AccountTaxRate.TAX_FREE
    )

    private fun holding(investmentId: Long, accountId: Long, ticker: String, quantity: String, currentPrice: String) =
        InvestmentWithTransactions(
            investment = Investment(
                id = investmentId,
                name = "Investment $ticker",
                ticker = ticker,
                category = InvestmentCategory.STOCK,
                currency = Currency.PLN,
                currentPrice = BigDecimal(currentPrice),
                accountId = accountId
            ),
            transactions = listOf(
                InvestmentTransaction(
                    type = InvestmentTransactionType.BUY,
                    date = LocalDate.parse("2024-01-01"),
                    quantity = BigDecimal(quantity),
                    pricePerUnit = BigDecimal(currentPrice)
                )
            )
        )

    @Test
    fun `a price increase raises balance and net by the same quantity-weighted amount`() {
        val holdingA = holding(investmentId = 1, accountId = 1, ticker = "TCK", quantity = "10", currentPrice = "100")
        val changes = computeAccountBalanceChanges(
            investmentsByAccount = mapOf(1L to listOf(holdingA)),
            accounts = listOf(account(id = 1, uninvestedCash = "500", balance = "1500")),
            priceUpdates = mapOf(1L to BigDecimal("110"))
        )

        val accountChange = changes.single()
        assertEquals(0, BigDecimal("1500").compareTo(accountChange.beforeAccountBalance)) // 500 cash + 10*100
        assertEquals(0, BigDecimal("1600").compareTo(accountChange.afterAccountBalance)) // 500 cash + 10*110
        assertEquals(0, BigDecimal("100").compareTo(accountChange.accountChange))
        assertEquals(0, accountChange.accountChange.compareTo(accountChange.netChange)) // tax-free account: net moves 1:1 with balance
        assertTrue(accountChange.accountChangePercent!! > BigDecimal.ZERO)

        val investmentChange = accountChange.investments.single()
        assertEquals(1L, investmentChange.investmentId)
        assertEquals(0, BigDecimal("100").compareTo(investmentChange.change))
    }

    @Test
    fun `an account with no price updates still appears with zero change, not excluded`() {
        val holdingA = holding(investmentId = 1, accountId = 1, ticker = "TCK", quantity = "10", currentPrice = "100")
        val changes = computeAccountBalanceChanges(
            investmentsByAccount = mapOf(1L to listOf(holdingA)),
            accounts = listOf(account(id = 1, balance = "1000")),
            priceUpdates = emptyMap()
        )

        val accountChange = changes.single()
        assertEquals(0, accountChange.beforeAccountBalance.compareTo(accountChange.afterAccountBalance))
        assertTrue(accountChange.investments.isEmpty())
    }

    @Test
    fun `an untouched account with a sub-cent-rounding position shows exactly zero change, not a phantom epsilon`() {
        // quantity * currentPrice has 3 decimal digits (100.005) — AccountRepositoryImpl.investmentsValue()
        // rounds that to 100.01 (HALF_UP) when computing the account's real stored balance. If the
        // freshly recomputed "after" value isn't rounded the same way, comparing the unrounded 100.005
        // against the already-rounded 100.01 leaves a fraction-of-a-cent phantom "loss" even though
        // nothing was actually edited.
        val holdingA = holding(investmentId = 1, accountId = 1, ticker = "TCK", quantity = "3", currentPrice = "33.335")
        val changes = computeAccountBalanceChanges(
            investmentsByAccount = mapOf(1L to listOf(holdingA)),
            accounts = listOf(account(id = 1, balance = "100.01")),
            priceUpdates = emptyMap()
        )

        val accountChange = changes.single()
        assertEquals(0, accountChange.accountChange.signum())
        assertEquals(0, accountChange.netChange.signum())
    }

    @Test
    fun `an account with no held positions is excluded entirely`() {
        val changes = computeAccountBalanceChanges(
            investmentsByAccount = emptyMap(),
            accounts = listOf(account(id = 1)),
            priceUpdates = emptyMap()
        )
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `only accounts passed in are considered, even if investmentsByAccount has more`() {
        val holdingA = holding(investmentId = 1, accountId = 1, ticker = "A", quantity = "10", currentPrice = "100")
        val holdingB = holding(investmentId = 2, accountId = 2, ticker = "B", quantity = "5", currentPrice = "50")
        val changes = computeAccountBalanceChanges(
            investmentsByAccount = mapOf(1L to listOf(holdingA), 2L to listOf(holdingB)),
            accounts = listOf(account(id = 1)),
            priceUpdates = mapOf(2L to BigDecimal("60"))
        )
        assertEquals(1, changes.size)
        assertEquals(1L, changes.single().accountId)
    }
}
