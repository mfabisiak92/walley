package com.walley.app.data.csv

import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BossaImportCsvParserTest {

    // Real BOSSA "historia operacji" export sample, header included, diacritics mangled as BOSSA emits them.
    private val sampleCsv = """
        data;papier;isin;iloúÊ;-;cena;wartoúÊ;prowizja;po prowizji
        09.07.2026 09:22:56;PKNORLEN;PLPKN0000018;34;S;138,82;4719,88;17,93;4701,95
        14.11.2024 11:37:45;PKNORLEN;PLPKN0000018;30;K;52,22;1566,60;5,95;1572,55
        08.12.2020 11:31:51;PCF GROUP_IPO;PLPCFGR00028;3;K;46,00;138,00;5,00;143,00
        01.12.2020 13:18:23;OPTEAM;PLOPTEM00012;29;K;33,00;957,00;0,00;957,00
    """.trimIndent()

    @Test
    fun `recognizes a BOSSA export by its header shape`() {
        assertTrue(looksLikeBossaExport(sampleCsv))
    }

    @Test
    fun `does not recognize the app's own comma-delimited schema as BOSSA`() {
        val ownSchema = "account,ticker,name,type,date,quantity,price\nXTB,AAPL,Apple Inc.,BUY,2026-01-03,8,750.00\n"
        assertTrue(!looksLikeBossaExport(ownSchema))
    }

    @Test
    fun `parses a sell row with Polish decimal separators and a dd-mm-yyyy datetime`() {
        val results = parseBossaExportCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val row = (results[0] as CsvRowParseResult.Parsed).row
        assertEquals(LocalDate.of(2026, 7, 9), row.date)
        assertEquals("PLPKN0000018", row.ticker)
        assertEquals("PKNORLEN", row.name)
        assertEquals(InvestmentTransactionType.SELL, row.type)
        assertEquals(0, BigDecimal("34").compareTo(row.quantity))
        assertEquals(0, BigDecimal("138.82").compareTo(row.price))
        assertEquals(0, BigDecimal("17.93").compareTo(row.commission))
    }

    @Test
    fun `K maps to BUY and S maps to SELL`() {
        val results = parseBossaExportCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val rows = results.map { (it as CsvRowParseResult.Parsed).row }
        assertEquals(InvestmentTransactionType.SELL, rows[0].type)
        assertEquals(InvestmentTransactionType.BUY, rows[1].type)
    }

    @Test
    fun `zero commission parses correctly`() {
        val results = parseBossaExportCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val opteamRow = results.map { (it as CsvRowParseResult.Parsed).row }.first { it.ticker == "PLOPTEM00012" }
        assertEquals(0, BigDecimal.ZERO.compareTo(opteamRow.commission))
    }

    @Test
    fun `a security name containing a space is preserved`() {
        val results = parseBossaExportCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val row = results.map { (it as CsvRowParseResult.Parsed).row }.first { it.ticker == "PLPCFGR00028" }
        assertEquals("PCF GROUP_IPO", row.name)
    }

    @Test
    fun `every row is assigned the caller's chosen account`() {
        val results = parseBossaExportCsv(sampleCsv, accountId = 42, accountName = "My BOSSA account")
        results.map { (it as CsvRowParseResult.Parsed).row }.forEach {
            assertEquals(42L, it.accountId)
            assertEquals("My BOSSA account", it.accountName)
        }
    }

    @Test
    fun `category defaults to STOCK since BOSSA doesn't provide one`() {
        val results = parseBossaExportCsv(sampleCsv, accountId = 1, accountName = "BOSSA")
        val row = (results.first() as CsvRowParseResult.Parsed).row
        assertEquals(com.walley.app.domain.model.InvestmentCategory.STOCK, row.category)
    }

    @Test
    fun `an unparseable date is rejected with a row number`() {
        val csv = "data;papier;isin;ilosc;-;cena;wartosc;prowizja;po prowizji\n" +
            "not-a-date;PKNORLEN;PLPKN0000018;34;S;138,82;4719,88;17,93;4701,95\n"
        val results = parseBossaExportCsv(csv, accountId = 1, accountName = "BOSSA")
        val error = results.single() as CsvRowParseResult.Invalid
        assertEquals(2, error.rowNumber)
    }
}
