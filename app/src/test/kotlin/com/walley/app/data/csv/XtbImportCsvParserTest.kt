package com.walley.app.data.csv

import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XtbImportCsvParserTest {

    // Trimmed real XTB "Cash Operations" export sample: metadata rows, header, a mix of trade and
    // non-trade rows (interest, transfer, deposit), two partial fills of the same order, and the
    // trailing summary row.
    private val sampleCsv = """
        Account number,51296707,,,,,,
        Cash Operations,,,,,,,
        Date from (UTC),2006-01-01 00:00:00,,,,,,
        Date to (UTC),2026-07-09 10:21:49,,,,,,
        Type,Ticker,Instrument,Time,Amount,ID,Comment,Product
        Free funds interest,,,2026-07-06 12:16:19,"2,88",1344408258,Free-funds Interest 2026-06,My Trades
        Free funds interest tax,,,2026-07-06 12:11:31,"-0,55",1344389871,Free-funds Interest Tax 2026-06,My Trades
        Stock purchase,MSTR.US,Strategy,2026-06-25 13:30:45,-2861,1328914046,OPEN BUY 8/8.2228 @ 94.42,My Trades
        Stock purchase,MSTR.US,Strategy,2026-06-25 13:30:02,"-79,98",1328865639,OPEN BUY 0.2228/8.2228 @ 94.76,My Trades
        Transfer,,,2026-06-12 08:50:01,500,1308837340,Transfer from 51296707 to 52725876,Investment Plans
        Deposit,,,2026-06-10 10:17:41,2000,1304496850,"Pekao S.A. deposit, id=33907006",My Trades
        Total,,,,"839,37",,,
    """.trimIndent()

    @Test
    fun `recognizes an XTB export by its first metadata line`() {
        assertTrue(looksLikeXtbCashOperationsExport(sampleCsv))
    }

    @Test
    fun `does not recognize BOSSA or the app's own schema as XTB`() {
        assertTrue(!looksLikeXtbCashOperationsExport("data;papier;isin;ilosc;-;cena;wartosc;prowizja;po prowizji\n"))
        assertTrue(!looksLikeXtbCashOperationsExport("account,ticker,name,type,date,quantity,price\n"))
    }

    @Test
    fun `only stock purchase and sale rows are imported, everything else is skipped`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB")
        assertEquals(2, results.size)
        results.forEach { assertTrue(it is CsvRowParseResult.Parsed) }
    }

    @Test
    fun `quantity comes from the fill fraction's numerator, not the total order size`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB")
        val rows = results.map { (it as CsvRowParseResult.Parsed).row }
        assertEquals(0, BigDecimal("8").compareTo(rows[0].quantity))
        assertEquals(0, BigDecimal("0.2228").compareTo(rows[1].quantity))
    }

    @Test
    fun `price is derived from amount over quantity, not the comment's foreign-currency price`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB")
        val row = (results[0] as CsvRowParseResult.Parsed).row
        val expected = BigDecimal("2861").divide(BigDecimal("8"), 8, RoundingMode.HALF_UP)
        assertEquals(0, expected.compareTo(row.price))
        // Sanity: derived PLN price should be nowhere near the USD "@ 94.42" figure from the comment.
        assertTrue(row.price > BigDecimal("300"))
    }

    @Test
    fun `date is parsed from the Time column, dropping the time portion`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB")
        val row = (results[0] as CsvRowParseResult.Parsed).row
        assertEquals(LocalDate.of(2026, 6, 25), row.date)
    }

    @Test
    fun `both fills of a split order are BUY and share the same ticker`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB")
        val rows = results.map { (it as CsvRowParseResult.Parsed).row }
        assertEquals("MSTR.US", rows[0].ticker)
        assertEquals("MSTR.US", rows[1].ticker)
        assertEquals(InvestmentTransactionType.BUY, rows[0].type)
        assertEquals(InvestmentTransactionType.BUY, rows[1].type)
    }

    @Test
    fun `commission is always zero since XTB doesn't break it out in this report`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB")
        results.forEach {
            assertEquals(0, BigDecimal.ZERO.compareTo((it as CsvRowParseResult.Parsed).row.commission))
        }
    }

    @Test
    fun `every row is assigned the caller's chosen account`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 99, accountName = "My XTB")
        results.forEach {
            val row = (it as CsvRowParseResult.Parsed).row
            assertEquals(99L, row.accountId)
            assertEquals("My XTB", row.accountName)
        }
    }

    @Test
    fun `account operations are still skipped when includeAccountOperations is false`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB", includeAccountOperations = false)
        assertEquals(2, results.size)
        results.forEach { assertTrue(it is CsvRowParseResult.Parsed) }
    }

    @Test
    fun `deposits, transfers and interest become cash operations when includeAccountOperations is true`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB", includeAccountOperations = true)
        // 2 trades + interest + interest tax + transfer + deposit = 6; the trailing Total row is still skipped.
        assertEquals(6, results.size)
        val cashOps = results.filterIsInstance<CsvRowParseResult.ParsedCashOperation>().map { it.row }
        assertEquals(4, cashOps.size)
        assertTrue(cashOps.none { it.description.startsWith("Total", ignoreCase = true) })
    }

    @Test
    fun `cash operation amount is signed and matches the CSV's Amount column`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 1, accountName = "XTB", includeAccountOperations = true)
        val deposit = results.filterIsInstance<CsvRowParseResult.ParsedCashOperation>()
            .map { it.row }
            .single { it.description.startsWith("Deposit") }
        assertEquals(0, BigDecimal("2000").compareTo(deposit.amount))
        val interestTax = results.filterIsInstance<CsvRowParseResult.ParsedCashOperation>()
            .map { it.row }
            .single { it.description.startsWith("Free funds interest tax") }
        assertEquals(0, BigDecimal("-0.55").compareTo(interestTax.amount))
    }

    @Test
    fun `cash operation rows carry the caller's chosen account`() {
        val results = parseXtbCashOperationsCsv(sampleCsv, accountId = 99, accountName = "My XTB", includeAccountOperations = true)
        results.filterIsInstance<CsvRowParseResult.ParsedCashOperation>().forEach {
            assertEquals(99L, it.row.accountId)
            assertEquals("My XTB", it.row.accountName)
        }
    }
}
