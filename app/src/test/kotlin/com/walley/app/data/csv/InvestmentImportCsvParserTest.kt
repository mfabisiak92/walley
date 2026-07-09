package com.walley.app.data.csv

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvestmentImportCsvParserTest {

    private val accounts = listOf(
        Account(id = 1, name = "XTB", type = AccountType.INVESTMENT, currency = Currency.PLN, balance = BigDecimal.ZERO)
    )

    @Test
    fun `parses a well-formed row with all optional fields present`() {
        val csv = "account,ticker,name,category,type,date,quantity,price,commission\n" +
            "XTB,AAPL,Apple Inc.,STOCK,BUY,2026-01-03,8,750.00,5.00\n"
        val results = parseInvestmentImportCsv(csv, accounts)
        val row = (results.single() as CsvRowParseResult.Parsed).row
        assertEquals(1L, row.accountId)
        assertEquals("AAPL", row.ticker)
        assertEquals(InvestmentCategory.STOCK, row.category)
        assertEquals(InvestmentTransactionType.BUY, row.type)
        assertEquals(0, BigDecimal("8").compareTo(row.quantity))
        assertEquals(0, BigDecimal("5.00").compareTo(row.commission))
    }

    @Test
    fun `category and commission default when blank`() {
        val csv = "account,ticker,name,type,date,quantity,price\n" +
            "XTB,AAPL,Apple Inc.,BUY,2026-01-03,8,750.00\n"
        val results = parseInvestmentImportCsv(csv, accounts)
        val row = (results.single() as CsvRowParseResult.Parsed).row
        assertEquals(InvestmentCategory.STOCK, row.category)
        assertEquals(0, BigDecimal.ZERO.compareTo(row.commission))
    }

    @Test
    fun `an unknown account name is rejected`() {
        val csv = "account,ticker,name,type,date,quantity,price\n" +
            "Unknown Broker,AAPL,Apple Inc.,BUY,2026-01-03,8,750.00\n"
        val results = parseInvestmentImportCsv(csv, accounts)
        assertTrue(results.single() is CsvRowParseResult.Invalid)
    }

    @Test
    fun `a non-ISO date is rejected`() {
        val csv = "account,ticker,name,type,date,quantity,price\n" +
            "XTB,AAPL,Apple Inc.,BUY,03/01/2026,8,750.00\n"
        val results = parseInvestmentImportCsv(csv, accounts)
        assertTrue(results.single() is CsvRowParseResult.Invalid)
    }

    @Test
    fun `a missing required column rejects the whole file`() {
        val csv = "account,ticker,name,type,quantity,price\nXTB,AAPL,Apple Inc.,BUY,8,750.00\n"
        val results = parseInvestmentImportCsv(csv, accounts)
        val error = results.single() as CsvRowParseResult.Invalid
        assertTrue(error.reason.contains("date"))
    }

    @Test
    fun `row numbers account for the header row`() {
        val csv = "account,ticker,name,type,date,quantity,price\n" +
            "XTB,AAPL,Apple Inc.,BUY,2026-01-03,8,750.00\n" +
            "XTB,CDR,CD Projekt,BUY,2026-01-04,3,300.00\n"
        val results = parseInvestmentImportCsv(csv, accounts)
        assertEquals(2, (results[0] as CsvRowParseResult.Parsed).row.rowNumber)
        assertEquals(3, (results[1] as CsvRowParseResult.Parsed).row.rowNumber)
    }
}
