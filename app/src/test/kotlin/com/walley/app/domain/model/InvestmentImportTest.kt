package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentImportTest {

    private fun account(id: Long = 1, uninvestedCash: String = "1000") = Account(
        id = id,
        name = "Brokerage $id",
        type = AccountType.INVESTMENT,
        currency = Currency.PLN,
        balance = BigDecimal(uninvestedCash),
        uninvestedCash = BigDecimal(uninvestedCash)
    )

    private fun row(
        rowNumber: Int,
        accountId: Long = 1,
        ticker: String = "TCK",
        type: InvestmentTransactionType = InvestmentTransactionType.BUY,
        date: String,
        quantity: String,
        price: String,
        commission: String = "0"
    ) = CsvRowParseResult.Parsed(
        ParsedImportRow(
            rowNumber = rowNumber,
            accountId = accountId,
            accountName = "Brokerage $accountId",
            ticker = ticker,
            name = "Investment",
            category = InvestmentCategory.STOCK,
            type = type,
            date = LocalDate.parse(date),
            quantity = BigDecimal(quantity),
            price = BigDecimal(price),
            commission = BigDecimal(commission)
        )
    )

    @Test
    fun `a valid buy within available cash is accepted`() {
        val outcomes = validateImportRows(
            listOf(row(1, date = "2026-01-01", quantity = "10", price = "20")),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap()
        )
        assertEquals(ImportRowStatus.ToImport, outcomes.single().status)
    }

    @Test
    fun `a buy exceeding the account's cash is rejected`() {
        val outcomes = validateImportRows(
            listOf(row(1, date = "2026-01-01", quantity = "1000", price = "20")),
            accounts = listOf(account(uninvestedCash = "1000")),
            investmentsByAccount = emptyMap()
        )
        assertTrue(outcomes.single().status is ImportRowStatus.Rejected)
    }

    @Test
    fun `a sell exceeding held quantity is rejected`() {
        val outcomes = validateImportRows(
            listOf(row(1, type = InvestmentTransactionType.SELL, date = "2026-01-01", quantity = "5", price = "20")),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap()
        )
        assertTrue(outcomes.single().status is ImportRowStatus.Rejected)
    }

    @Test
    fun `a sell matching an earlier buy in the same batch is accepted`() {
        val outcomes = validateImportRows(
            listOf(
                row(1, date = "2026-01-01", quantity = "10", price = "20"),
                row(2, type = InvestmentTransactionType.SELL, date = "2026-02-01", quantity = "4", price = "25")
            ),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap()
        )
        assertEquals(ImportRowStatus.ToImport, outcomes[0].status)
        assertEquals(ImportRowStatus.ToImport, outcomes[1].status)
    }

    @Test
    fun `rows are validated in chronological order regardless of file order`() {
        // Sell listed first in the file, but dated after the buy -> still valid once replayed by date.
        val outcomes = validateImportRows(
            listOf(
                row(1, type = InvestmentTransactionType.SELL, date = "2026-02-01", quantity = "4", price = "25"),
                row(2, date = "2026-01-01", quantity = "10", price = "20")
            ),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap()
        )
        assertEquals(ImportRowStatus.ToImport, outcomes.first { it.rowNumber == 1 }.status)
        assertEquals(ImportRowStatus.ToImport, outcomes.first { it.rowNumber == 2 }.status)
    }

    @Test
    fun `a rejected buy cascades to reject a later sell of the same never-bought lot`() {
        val outcomes = validateImportRows(
            listOf(
                row(1, date = "2026-01-01", quantity = "1000", price = "20"),
                row(2, type = InvestmentTransactionType.SELL, date = "2026-02-01", quantity = "4", price = "25")
            ),
            accounts = listOf(account(uninvestedCash = "1000")),
            investmentsByAccount = emptyMap()
        )
        assertTrue(outcomes[0].status is ImportRowStatus.Rejected)
        assertTrue(outcomes[1].status is ImportRowStatus.Rejected)
    }

    @Test
    fun `a row matching an existing transaction exactly is treated as a duplicate`() {
        val existing = InvestmentWithTransactions(
            investment = Investment(
                name = "Investment",
                ticker = "TCK",
                category = InvestmentCategory.STOCK,
                currency = Currency.PLN,
                currentPrice = BigDecimal("20"),
                accountId = 1
            ),
            transactions = listOf(
                InvestmentTransaction(
                    type = InvestmentTransactionType.BUY,
                    date = LocalDate.parse("2026-01-01"),
                    quantity = BigDecimal("10"),
                    pricePerUnit = BigDecimal("20")
                )
            )
        )
        val outcomes = validateImportRows(
            listOf(row(1, date = "2026-01-01", quantity = "10", price = "20")),
            accounts = listOf(account()),
            investmentsByAccount = mapOf(1L to listOf(existing))
        )
        assertEquals(ImportRowStatus.Duplicate, outcomes.single().status)
    }

    @Test
    fun `duplicates are also detected within the same batch, not just against existing data`() {
        val outcomes = validateImportRows(
            listOf(
                row(1, date = "2026-01-01", quantity = "10", price = "20"),
                row(2, date = "2026-01-01", quantity = "10", price = "20")
            ),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap()
        )
        assertEquals(ImportRowStatus.ToImport, outcomes[0].status)
        assertEquals(ImportRowStatus.Duplicate, outcomes[1].status)
    }

    @Test
    fun `invalid parse results surface as rejected outcomes preserving row order`() {
        val outcomes = validateImportRows(
            listOf(
                row(1, date = "2026-01-01", quantity = "10", price = "20"),
                CsvRowParseResult.Invalid(2, "Unknown category \"BOND\"")
            ),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap()
        )
        assertEquals(2, outcomes.size)
        assertEquals(1, outcomes[0].rowNumber)
        assertEquals(2, outcomes[1].rowNumber)
        assertTrue(outcomes[1].status is ImportRowStatus.Rejected)
        assertEquals("Unknown category \"BOND\"", (outcomes[1].status as ImportRowStatus.Rejected).reason)
    }
}
