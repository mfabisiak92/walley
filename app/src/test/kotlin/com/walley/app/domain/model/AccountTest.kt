package com.walley.app.domain.model

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountTest {

    private fun investmentAccount(
        balance: BigDecimal,
        uninvestedCash: BigDecimal,
        investmentCostBasis: BigDecimal,
        taxRate: AccountTaxRate = AccountTaxRate.STANDARD_19
    ) = Account(
        name = "Brokerage",
        type = AccountType.INVESTMENT,
        currency = Currency.PLN,
        balance = balance,
        taxRate = taxRate,
        uninvestedCash = uninvestedCash,
        investmentCostBasis = investmentCostBasis
    )

    @Test
    fun `computes net profit and tax when there is a taxable gain`() {
        // current value = 1500 (balance 1600 - uninvested 100), cost basis 1000 -> gain 500
        val account = investmentAccount(
            balance = BigDecimal("1600"),
            uninvestedCash = BigDecimal("100"),
            investmentCostBasis = BigDecimal("1000")
        )
        assertEquals(BigDecimal("500"), account.investmentGainLoss)
        assertEquals(BigDecimal("95.00"), account.investmentTaxAmount)
        assertEquals(BigDecimal("405.00"), account.investmentNetProfit)
    }

    @Test
    fun `no tax or net profit when the account is tax-free`() {
        val account = investmentAccount(
            balance = BigDecimal("1600"),
            uninvestedCash = BigDecimal("100"),
            investmentCostBasis = BigDecimal("1000"),
            taxRate = AccountTaxRate.TAX_FREE
        )
        assertEquals(BigDecimal("500"), account.investmentGainLoss)
        assertNull(account.investmentTaxAmount)
        assertNull(account.investmentNetProfit)
    }

    @Test
    fun `no tax or net profit on a loss`() {
        val account = investmentAccount(
            balance = BigDecimal("800"),
            uninvestedCash = BigDecimal("100"),
            investmentCostBasis = BigDecimal("1000")
        )
        assertEquals(BigDecimal("-300"), account.investmentGainLoss)
        assertNull(account.investmentTaxAmount)
        assertNull(account.investmentNetProfit)
    }

    @Test
    fun `non-investment accounts never get a phantom gain or tax`() {
        // A plain Checking account's balance would otherwise look like a taxable "gain" since
        // uninvestedCash and investmentCostBasis both default to zero.
        val account = Account(
            name = "Checking",
            type = AccountType.CHECKING,
            currency = Currency.PLN,
            balance = BigDecimal("1000")
        )
        assertEquals(BigDecimal.ZERO, account.investmentGainLoss)
        assertNull(account.investmentTaxAmount)
        assertNull(account.investmentNetProfit)
        assertEquals(BigDecimal("1000"), account.netWorthValue)
    }

    @Test
    fun `netWorthValue subtracts tax owed on a taxable gain`() {
        val account = investmentAccount(
            balance = BigDecimal("1600"),
            uninvestedCash = BigDecimal("100"),
            investmentCostBasis = BigDecimal("1000")
        )
        assertEquals(BigDecimal("1505.00"), account.netWorthValue)
    }

    @Test
    fun `netWorthValue equals balance when there is no taxable gain`() {
        val account = investmentAccount(
            balance = BigDecimal("800"),
            uninvestedCash = BigDecimal("100"),
            investmentCostBasis = BigDecimal("1000")
        )
        assertEquals(BigDecimal("800"), account.netWorthValue)
    }
}
