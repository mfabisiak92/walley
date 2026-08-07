package com.walley.app.feature.investments

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ImportRowOutcome
import com.walley.app.domain.model.ImportRowStatus
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.ParsedCashOperationRow
import com.walley.app.domain.model.ParsedImportRow
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportBalanceReviewTest {

    private fun account(id: Long = 1, uninvestedCash: String = "0") = Account(
        id = id,
        name = "Brokerage $id",
        type = AccountType.INVESTMENT,
        currency = Currency.PLN,
        balance = BigDecimal(uninvestedCash),
        uninvestedCash = BigDecimal(uninvestedCash),
        taxRate = AccountTaxRate.TAX_FREE
    )

    private fun tradeOutcome(
        rowNumber: Int,
        accountId: Long = 1,
        ticker: String = "TCK",
        type: InvestmentTransactionType = InvestmentTransactionType.BUY,
        date: String,
        quantity: String,
        price: String,
        commission: String = "0"
    ) = ImportRowOutcome(
        rowNumber = rowNumber,
        row = ParsedImportRow(
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
        ),
        status = ImportRowStatus.ToImport
    )

    private fun cashOutcome(rowNumber: Int, accountId: Long = 1, date: String, amount: String, description: String = "Deposit") =
        ImportRowOutcome(
            rowNumber = rowNumber,
            cashOperation = ParsedCashOperationRow(
                rowNumber = rowNumber,
                accountId = accountId,
                accountName = "Brokerage $accountId",
                date = LocalDate.parse(date),
                description = description,
                amount = BigDecimal(amount)
            ),
            status = ImportRowStatus.ToImport
        )

    @Test
    fun `a deposit and a funded buy show up as balance-changing rows, and the deposit counts as cash added`() {
        val outcomes = listOf(
            cashOutcome(1, date = "2026-01-01", amount = "1000"),
            tradeOutcome(2, date = "2026-01-02", quantity = "10", price = "50")
        )
        val review = computeImportBalanceReview(
            outcomes,
            accounts = listOf(account(uninvestedCash = "0")),
            investmentsByAccount = emptyMap(),
            includeAccountOperations = true
        )

        val summary = review.accountSummaries.single()
        assertEquals(0, BigDecimal("1000").compareTo(summary.netCashAdded))
        // The buy costs exactly what the new position is now worth (current price defaults to the
        // trade price for a brand-new position), so it's balance-neutral -> the only overall change
        // is the deposit itself.
        assertEquals(0, BigDecimal("1000").compareTo(summary.afterBalance - summary.beforeBalance))

        val depositChange = review.rowChangesByRowNumber.getValue(1)
        assertTrue(depositChange.balanceChanged)
        assertEquals(0, BigDecimal("1000").compareTo(depositChange.afterBalance - depositChange.beforeBalance))

        val buyChange = review.rowChangesByRowNumber.getValue(2)
        // Cash drops by 500 but the new position's value rises by the same 500 -> total balance flat.
        assertEquals(0, buyChange.beforeBalance.compareTo(buyChange.afterBalance))
    }

    @Test
    fun `without includeAccountOperations a buy doesn't move the account's cash or balance`() {
        val outcomes = listOf(tradeOutcome(1, date = "2026-01-02", quantity = "10", price = "50"))
        val review = computeImportBalanceReview(
            outcomes,
            accounts = listOf(account(uninvestedCash = "1000")),
            investmentsByAccount = emptyMap(),
            includeAccountOperations = false
        )

        val buyChange = review.rowChangesByRowNumber.getValue(1)
        // Cash stays put, but the new position still adds its own value on top -> balance still rises.
        assertTrue(buyChange.balanceChanged)
        assertEquals(0, BigDecimal("500").compareTo(buyChange.afterBalance - buyChange.beforeBalance))
        assertEquals(0, review.accountSummaries.single().netCashAdded.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun `only accepted rows for an untouched account produce no summary`() {
        val review = computeImportBalanceReview(
            outcomes = emptyList(),
            accounts = listOf(account()),
            investmentsByAccount = emptyMap(),
            includeAccountOperations = true
        )
        assertTrue(review.accountSummaries.isEmpty())
        assertTrue(review.rowChangesByRowNumber.isEmpty())
    }
}
