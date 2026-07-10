package com.walley.app.data.csv

import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BossaCashOperationsCsvParserTest {

    // Trimmed real BOSSA (DM BOŚ) "historia operacji pieniężnych" export sample: header, a mix of
    // trade settlement rows (buy and sell) and non-trade rows (dividend, dividend tax, transfer).
    private val sampleCsv = """
        data;tytuł operacji;szczegóły;kwota
        2026-07-09;Wypłata dywidendy KGHM;;60,00
        2026-07-09;Podatek od odsetek lub dywidendy KGHM;;-11,00
        2026-07-09;Rozliczenie transakcji sprzedaży:;PKNORLEN (PLPKN0000018) 34 x 138.82 PLN nr 000000001165;4701,95
        2024-11-14;Rozliczenie transakcji kupna:;PKNORLEN (PLPKN0000018) 30 x 52.22 PLN nr 000000011266;-1572,55
        2026-02-24;Przelew do DM BOŚ;;3000,00
    """.trimIndent()

    // Same rows as sampleCsv, but comma-delimited with decimal-comma amounts quoted to protect them —
    // what you get after a spreadsheet round-trip, e.g. opening and re-saving the export in Excel.
    private val commaDelimitedSampleCsv = """
        data,tytuł operacji,szczegóły,kwota
        2026-07-09,Wypłata dywidendy KGHM,,60
        2026-07-09,Podatek od odsetek lub dywidendy KGHM,,-11
        2026-07-09,Rozliczenie transakcji sprzedaży:,PKNORLEN (PLPKN0000018) 34 x 138.82 PLN nr 000000001165,"4701,95"
        2024-11-14,Rozliczenie transakcji kupna:,PKNORLEN (PLPKN0000018) 30 x 52.22 PLN nr 000000011266,"-1572,55"
        2026-02-24,Przelew do DM BOŚ,,3000
    """.trimIndent()

    @Test
    fun `recognizes a BOSSA cash operations export by its 4-column data header`() {
        assertTrue(looksLikeBossaCashOperationsExport(sampleCsv))
    }

    @Test
    fun `recognizes the same export when comma-delimited with quoted decimal-comma amounts`() {
        assertTrue(looksLikeBossaCashOperationsExport(commaDelimitedSampleCsv))
    }

    @Test
    fun `parses identically whether the file is semicolon- or comma-delimited`() {
        val fromSemicolons = parseBossaCashOperationsCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val fromCommas = parseBossaCashOperationsCsv(commaDelimitedSampleCsv, accountId = 1, accountName = "BOSSA")
        assertEquals(2, fromCommas.size)
        val rowsFromSemicolons = fromSemicolons.map { (it as CsvRowParseResult.Parsed).row }
        val rowsFromCommas = fromCommas.map { (it as CsvRowParseResult.Parsed).row }
        rowsFromSemicolons.zip(rowsFromCommas).forEach { (a, b) ->
            assertEquals(a.ticker, b.ticker)
            assertEquals(a.type, b.type)
            assertEquals(0, a.quantity.compareTo(b.quantity))
            assertEquals(0, a.price.compareTo(b.price))
            assertEquals(0, a.commission.compareTo(b.commission))
        }
    }

    // A newer export variant with a trailing `waluta` (currency) column and alphanumeric order numbers
    // (e.g. "nr Z00357144519" instead of a purely numeric one) — both should still parse.
    private val fiveColumnSampleCsv = """
        data;tytuł operacji;szczegóły;kwota;waluta
        2026-06-25;Przelew do DM BOŚ;;3623,87;PLN
        2026-06-02;Rozliczenie transakcji kupna:;Cameco Corp. (CA13321L1085) 5 x 409.9499 PLN nr Z00357144519;-2063,75;PLN
        2026-04-30;Rozliczenie transakcji sprzedaży:;WisdomTree Brent Crude Oil  (JE00B78CGV99) 10 x 342.7115 PLN nr Z00340278854;3427,12;PLN
    """.trimIndent()

    @Test
    fun `recognizes an export with an extra trailing waluta column`() {
        assertTrue(looksLikeBossaCashOperationsExport(fiveColumnSampleCsv))
    }

    @Test
    fun `parses trade rows with alphanumeric order numbers and a trailing currency column`() {
        val results = parseBossaCashOperationsCsv(fiveColumnSampleCsv, accountId = 1, accountName = "BOSSA")
        assertEquals(2, results.size)
        val rows = results.map { (it as CsvRowParseResult.Parsed).row }

        val buy = rows.single { it.type == InvestmentTransactionType.BUY }
        assertEquals("CA13321L1085", buy.ticker)
        assertEquals("Cameco Corp.", buy.name)
        assertEquals(0, BigDecimal("5").compareTo(buy.quantity))

        // Company name has two spaces before the ISIN in the source file.
        val sell = rows.single { it.type == InvestmentTransactionType.SELL }
        assertEquals("JE00B78CGV99", sell.ticker)
        assertEquals("WisdomTree Brent Crude Oil", sell.name)
    }

    @Test
    fun `does not recognize the dedicated BOSSA trade export or XTB as this format`() {
        assertTrue(
            !looksLikeBossaCashOperationsExport("data;papier;isin;ilosc;-;cena;wartosc;prowizja;po prowizji\n")
        )
        assertTrue(!looksLikeBossaCashOperationsExport("Account number,51296707,,,,,,\n"))
    }

    @Test
    fun `only buy and sell settlement rows are imported by default, everything else is skipped`() {
        val results = parseBossaCashOperationsCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        assertEquals(2, results.size)
        results.forEach { assertTrue(it is CsvRowParseResult.Parsed) }
    }

    @Test
    fun `ticker is set to the ISIN and name to the short label, matching the dedicated BOSSA parser`() {
        val results = parseBossaCashOperationsCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val rows = results.map { (it as CsvRowParseResult.Parsed).row }
        assertEquals("PLPKN0000018", rows[0].ticker)
        assertEquals("PKNORLEN", rows[0].name)
    }

    @Test
    fun `commission is derived from the gap between gross trade value and the settled amount`() {
        val results = parseBossaCashOperationsCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val rows = results.map { (it as CsvRowParseResult.Parsed).row }

        val sell = rows.single { it.type == InvestmentTransactionType.SELL }
        // gross = 34 * 138.82 = 4719.88, settled = 4701.95 -> commission = 17.93
        assertEquals(0, BigDecimal("17.93").compareTo(sell.commission))

        val buy = rows.single { it.type == InvestmentTransactionType.BUY }
        // gross = 30 * 52.22 = 1566.60, settled = -1572.55 -> commission = 5.95
        assertEquals(0, BigDecimal("5.95").compareTo(buy.commission))
    }

    @Test
    fun `date is parsed from the first column`() {
        val results = parseBossaCashOperationsCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val row = (results[0] as CsvRowParseResult.Parsed).row
        assertEquals(LocalDate.of(2026, 7, 9), row.date)
    }

    @Test
    fun `non-trade rows become cash operations when includeAccountOperations is true`() {
        val results = parseBossaCashOperationsCsv(sampleCsv, accountId = 1, accountName = "BOSSA", includeAccountOperations = true)
        // 2 trades + dividend + dividend tax + deposit = 5.
        assertEquals(5, results.size)
        val cashOps = results.filterIsInstance<CsvRowParseResult.ParsedCashOperation>().map { it.row }
        assertEquals(3, cashOps.size)
        val deposit = cashOps.single { it.description.startsWith("Przelew do DM BO") }
        assertEquals(0, BigDecimal("3000").compareTo(deposit.amount))
        val tax = cashOps.single { it.description.startsWith("Podatek") }
        assertEquals(0, BigDecimal("-11").compareTo(tax.amount))
    }

    @Test
    fun `every row is assigned the caller's chosen account`() {
        val results = parseBossaCashOperationsCsv(sampleCsv, accountId = 99, accountName = "My BOSSA", includeAccountOperations = true)
        results.forEach {
            when (it) {
                is CsvRowParseResult.Parsed -> {
                    assertEquals(99L, it.row.accountId)
                    assertEquals("My BOSSA", it.row.accountName)
                }
                is CsvRowParseResult.ParsedCashOperation -> {
                    assertEquals(99L, it.row.accountId)
                    assertEquals("My BOSSA", it.row.accountName)
                }
                is CsvRowParseResult.Invalid -> Unit
            }
        }
    }
}
