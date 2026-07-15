package com.walley.app.feature.accounts

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.SortDirection
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountsFilteringTest {

    private val rates = ExchangeRates(
        base = Currency.PLN,
        rates = mapOf(Currency.EUR to BigDecimal("0.25")),
        date = "2026-01-01"
    )

    private fun account(
        id: Long,
        name: String,
        type: AccountType = AccountType.CHECKING,
        currency: Currency = Currency.PLN,
        balance: String = "0",
        isDefault: Boolean = false,
        isVirtual: Boolean = false,
        isClosed: Boolean = false
    ) = Account(
        id = id,
        name = name,
        type = type,
        currency = currency,
        balance = BigDecimal(balance),
        isDefault = isDefault,
        isVirtual = isVirtual,
        isClosed = isClosed
    )

    @Test
    fun `filterAccounts default status excludes closed accounts`() {
        val accounts = listOf(account(1, "Open"), account(2, "Closed", isClosed = true))
        val result = filterAccounts(accounts, AccountsFilterState())
        assertEquals(listOf("Open"), result.map { it.name })
    }

    @Test
    fun `filterAccounts CLOSED status keeps only closed accounts`() {
        val accounts = listOf(account(1, "Open"), account(2, "Closed", isClosed = true))
        val result = filterAccounts(accounts, AccountsFilterState(status = AccountStatusFilter.CLOSED))
        assertEquals(listOf("Closed"), result.map { it.name })
    }

    @Test
    fun `filterAccounts empty currency set matches everything`() {
        val accounts = listOf(account(1, "PLN", currency = Currency.PLN), account(2, "EUR", currency = Currency.EUR))
        val result = filterAccounts(accounts, AccountsFilterState())
        assertEquals(2, result.size)
    }

    @Test
    fun `filterAccounts non-empty currency set only matches selected currencies`() {
        val accounts = listOf(account(1, "PLN", currency = Currency.PLN), account(2, "EUR", currency = Currency.EUR))
        val result = filterAccounts(accounts, AccountsFilterState(currencies = setOf(Currency.EUR)))
        assertEquals(listOf("EUR"), result.map { it.name })
    }

    @Test
    fun `filterAccounts kind REAL excludes virtual accounts`() {
        val accounts = listOf(account(1, "Real"), account(2, "Virtual", isVirtual = true))
        val result = filterAccounts(accounts, AccountsFilterState(status = AccountStatusFilter.ALL, kind = AccountKindFilter.REAL))
        assertEquals(listOf("Real"), result.map { it.name })
    }

    @Test
    fun `sortAccounts NAME ASC sorts case-insensitively`() {
        val accounts = listOf(account(1, "banana"), account(2, "Apple"))
        val result = sortAccounts(accounts, AccountsSortState(AccountSortField.NAME, SortDirection.ASC), Currency.PLN, null)
        assertEquals(listOf("Apple", "banana"), result.map { it.name })
    }

    @Test
    fun `sortAccounts BALANCE DESC converts to base currency before comparing`() {
        // 100 EUR at rate 0.25 (1 PLN = 0.25 EUR) -> 400 PLN, which beats the 300 PLN account.
        val accounts = listOf(
            account(1, "PLN account", currency = Currency.PLN, balance = "300"),
            account(2, "EUR account", currency = Currency.EUR, balance = "100")
        )
        val result = sortAccounts(accounts, AccountsSortState(AccountSortField.BALANCE, SortDirection.DESC), Currency.PLN, rates)
        assertEquals(listOf("EUR account", "PLN account"), result.map { it.name })
    }

    @Test
    fun `sortAccounts BALANCE falls back to raw balance when a rate is missing`() {
        val accounts = listOf(
            account(1, "GBP account", currency = Currency.GBP, balance = "50"),
            account(2, "PLN account", currency = Currency.PLN, balance = "10")
        )
        val result = sortAccounts(accounts, AccountsSortState(AccountSortField.BALANCE, SortDirection.DESC), Currency.PLN, rates)
        assertEquals(listOf("GBP account", "PLN account"), result.map { it.name })
    }

    @Test
    fun `sortAccounts DATE_ADDED NEWEST first is DESC by id`() {
        val accounts = listOf(account(1, "First"), account(2, "Second"), account(3, "Third"))
        val result = sortAccounts(accounts, AccountsSortState(AccountSortField.DATE_ADDED, SortDirection.DESC), Currency.PLN, null)
        assertEquals(listOf("Third", "Second", "First"), result.map { it.name })
    }

    @Test
    fun `sortAccounts DEFAULT_FIRST pins the default account regardless of direction`() {
        val accounts = listOf(account(1, "Zebra"), account(2, "Alpha", isDefault = true))
        val result = sortAccounts(accounts, AccountsSortState(AccountSortField.DEFAULT_FIRST, SortDirection.ASC), Currency.PLN, null)
        assertEquals(listOf("Alpha", "Zebra"), result.map { it.name })
    }
}
