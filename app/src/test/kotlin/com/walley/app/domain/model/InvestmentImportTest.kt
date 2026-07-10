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

    private fun cashOp(
        rowNumber: Int,
        accountId: Long = 1,
        date: String,
        amount: String,
        description: String = "Deposit"
    ) = CsvRowParseResult.ParsedCashOperation(
        ParsedCashOperationRow(
            rowNumber = rowNumber,
            accountId = accountId,
            accountName = "Brokerage $accountId",
            date = LocalDate.parse(date),
            description = description,
            amount = BigDecimal(amount)
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

    @Test
    fun `a deposit funds a same-day buy even when the account starts at zero balance`() {
        val outcomes = validateImportRows(
            listOf(
                cashOp(1, date = "2026-01-01", amount = "1000"),
                row(2, date = "2026-01-01", quantity = "10", price = "50")
            ),
            accounts = listOf(account(uninvestedCash = "0")),
            investmentsByAccount = emptyMap()
        )
        assertEquals(ImportRowStatus.ToImport, outcomes.first { it.rowNumber == 1 }.status)
        assertEquals(ImportRowStatus.ToImport, outcomes.first { it.rowNumber == 2 }.status)
    }

    @Test
    fun `a buy dated before its funding deposit is still rejected`() {
        val outcomes = validateImportRows(
            listOf(
                row(1, date = "2026-01-01", quantity = "10", price = "50"),
                cashOp(2, date = "2026-01-02", amount = "1000")
            ),
            accounts = listOf(account(uninvestedCash = "0")),
            investmentsByAccount = emptyMap()
        )
        assertTrue(outcomes.first { it.rowNumber == 1 }.status is ImportRowStatus.Rejected)
        assertEquals(ImportRowStatus.ToImport, outcomes.first { it.rowNumber == 2 }.status)
    }

    @Test
    fun `a withdrawal reduces cash available for a later buy`() {
        val outcomes = validateImportRows(
            listOf(
                cashOp(1, date = "2026-01-01", amount = "-800", description = "Transfer"),
                row(2, date = "2026-01-02", quantity = "10", price = "50")
            ),
            accounts = listOf(account(uninvestedCash = "1000")),
            investmentsByAccount = emptyMap()
        )
        assertEquals(ImportRowStatus.ToImport, outcomes.first { it.rowNumber == 1 }.status)
        assertTrue(outcomes.first { it.rowNumber == 2 }.status is ImportRowStatus.Rejected)
    }

    @Test
    fun `cash operations against a non-existent account are rejected`() {
        val outcomes = validateImportRows(
            listOf(cashOp(1, accountId = 42, date = "2026-01-01", amount = "1000")),
            accounts = listOf(account(id = 1)),
            investmentsByAccount = emptyMap()
        )
        assertTrue(outcomes.single().status is ImportRowStatus.Rejected)
    }

    @Test
    fun `outcome order mirrors the original parse result order, not chronological order`() {
        val outcomes = validateImportRows(
            listOf(
                row(1, date = "2026-02-01", quantity = "1", price = "10"),
                cashOp(2, date = "2026-01-01", amount = "1000")
            ),
            accounts = listOf(account(uninvestedCash = "0")),
            investmentsByAccount = emptyMap()
        )
        assertEquals(listOf(1, 2), outcomes.map { it.rowNumber })
    }

    @Test
    fun `a cash operation matching an existing ledger entry exactly is treated as a duplicate`() {
        val existing = AccountOperation(accountId = 1, date = LocalDate.parse("2026-01-01"), description = "Deposit", amount = BigDecimal("1000"))
        val outcomes = validateImportRows(
            listOf(cashOp(1, date = "2026-01-01", amount = "1000")),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap(),
            accountOperationsByAccount = mapOf(1L to listOf(existing))
        )
        assertEquals(ImportRowStatus.Duplicate, outcomes.single().status)
    }

    @Test
    fun `a duplicate cash operation doesn't fund a later buy`() {
        val existing = AccountOperation(accountId = 1, date = LocalDate.parse("2026-01-01"), description = "Deposit", amount = BigDecimal("1000"))
        val outcomes = validateImportRows(
            listOf(
                cashOp(1, date = "2026-01-01", amount = "1000"),
                row(2, date = "2026-01-02", quantity = "10", price = "50")
            ),
            accounts = listOf(account(uninvestedCash = "0")),
            investmentsByAccount = emptyMap(),
            accountOperationsByAccount = mapOf(1L to listOf(existing))
        )
        assertEquals(ImportRowStatus.Duplicate, outcomes.first { it.rowNumber == 1 }.status)
        assertTrue(outcomes.first { it.rowNumber == 2 }.status is ImportRowStatus.Rejected)
    }

    @Test
    fun `cash operations that differ in date, description, or amount are not duplicates`() {
        val existing = AccountOperation(accountId = 1, date = LocalDate.parse("2026-01-01"), description = "Deposit", amount = BigDecimal("1000"))
        val outcomes = validateImportRows(
            listOf(
                cashOp(1, date = "2026-01-02", amount = "1000"),
                cashOp(2, date = "2026-01-01", amount = "1000", description = "Interest"),
                cashOp(3, date = "2026-01-01", amount = "999")
            ),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap(),
            accountOperationsByAccount = mapOf(1L to listOf(existing))
        )
        outcomes.forEach { assertEquals(ImportRowStatus.ToImport, it.status) }
    }

    @Test
    fun `duplicate cash operations are also detected within the same batch`() {
        val outcomes = validateImportRows(
            listOf(
                cashOp(1, date = "2026-01-01", amount = "1000"),
                cashOp(2, date = "2026-01-01", amount = "1000")
            ),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap()
        )
        assertEquals(ImportRowStatus.ToImport, outcomes[0].status)
        assertEquals(ImportRowStatus.Duplicate, outcomes[1].status)
    }
}
