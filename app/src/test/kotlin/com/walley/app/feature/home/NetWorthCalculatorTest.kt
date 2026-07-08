package com.walley.app.feature.home

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.Liability
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetWorthCalculatorTest {

    private val rates = ExchangeRates(
        base = Currency.PLN,
        rates = mapOf(Currency.EUR to BigDecimal("0.25")),
        date = "2026-01-01"
    )

    private fun account(balance: String, currency: Currency = Currency.PLN) = Account(
        name = "Account",
        type = AccountType.CHECKING,
        currency = currency,
        balance = BigDecimal(balance)
    )

    private fun asset(currentValue: String) = Asset(
        name = "Asset",
        currency = Currency.PLN,
        purchaseValue = BigDecimal(currentValue),
        currentValue = BigDecimal(currentValue),
        purchaseDate = LocalDate.now()
    )

    private fun liability(balance: String) = Liability(
        name = "Liability",
        currency = Currency.PLN,
        originalAmount = BigDecimal(balance),
        currentBalance = BigDecimal(balance),
        startDate = LocalDate.now()
    )

    @Test
    fun `sums accounts and assets, subtracts liabilities`() {
        val result = calculateNetWorth(
            accounts = listOf(account("1000")),
            assets = listOf(asset("500")),
            liabilities = listOf(liability("300")),
            targetCurrency = Currency.PLN,
            rates = rates
        )
        assertEquals(BigDecimal("1200"), result)
    }

    @Test
    fun `converts foreign-currency accounts using the given rates`() {
        // 100 EUR at rate 0.25 (1 PLN = 0.25 EUR) -> 400 PLN
        val result = calculateNetWorth(
            accounts = listOf(account("100", Currency.EUR)),
            assets = emptyList(),
            liabilities = emptyList(),
            targetCurrency = Currency.PLN,
            rates = rates
        )
        assertEquals(0, BigDecimal("400").compareTo(result))
    }

    @Test
    fun `returns null when a needed rate is unavailable`() {
        val result = calculateNetWorth(
            accounts = listOf(account("100", Currency.GBP)),
            assets = emptyList(),
            liabilities = emptyList(),
            targetCurrency = Currency.PLN,
            rates = rates
        )
        assertNull(result)
    }

    @Test
    fun `is zero for no accounts, assets, or liabilities`() {
        val result = calculateNetWorth(
            accounts = emptyList(),
            assets = emptyList(),
            liabilities = emptyList(),
            targetCurrency = Currency.PLN,
            rates = rates
        )
        assertEquals(BigDecimal.ZERO, result)
    }
}
