package com.walley.app.data.csv

import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObligacjeSkarboweImportCsvParserTest {

    // Trimmed real obligacjeskarbowe.pl "historia transakcji" export: a plain purchase, an early
    // redemption (order + confirmation + net-zero fee noise + an unexplained fee), a maturity rolled
    // straight into a new bond (reported with the real quantity on the *first* of the paired rows),
    // and a second rollover reported with the real quantity on the *second* row instead.
    private val sampleCsv = """
        DATA DYSPOZYCJI,RODZAJ DYSPOZYCJI,KOD OBLIGACJI,NR ZAPISU,SERIA,LICZBA OBLIGACJI,KWOTA OPERACJI,STATUS,UWAGI
        2019-12-31,dyspozycja zakupu,COI1223,10803,31,142,"14 200,00",zrealizowana,
        2019-12-31,zakup papierów,COI1223,10803,31,142,"14 200,00",zrealizowana,
        2020-12-23,naliczenie odsetek na 2020-12-31,COI1223,0,31,0,"340,80",zrealizowana,
        2020-12-31,dyspozycja zakupu,COI1224,10343,31,4,"400,00",zrealizowana,
        2020-12-31,zakup papierów,COI1224,10343,31,4,"400,00",zrealizowana,
        2021-02-23,dyspozycja przedterminowego wykupu,COI1224,0,31,1,"0,00",zrealizowana,
        2021-03-03,przedterminowy wykup,COI1224,0,31,0,"100,00",zrealizowana,
        ,odsetki,COI1224,0,,0,"0,22",zrealizowana,
        ,opłata za przedterminowy wykup,COI1224,0,,0,"-0,22",zrealizowana,
        2021-03-03,opłata - interwencyjny wykup,COI1224,0,,0,"3,56",zrealizowana,
        2023-12-21,naliczenie wykupu na 2023-12-31,COI1223,4,31,0,"14 200,00",zrealizowana,
        2023-12-21,wykup - odsetki,COI1223,4,31,0,"2 719,30",zrealizowana,
        2023-12-27,zamiana obligacji,COI1223,0,31,142,"14 200,00",zrealizowana,
        2023-12-27,zamiana obligacji,COI1223,0,31,0,"14 200,00",zrealizowana,
        2023-12-31,zakup papierów,COI1227,26411,31,169,"16 883,10",zrealizowana,
        2024-01-02,zakup papierów - zamiana,COI1223,0,31,0,"16 883,10",zrealizowana,
        2025-04-25,zamiana obligacji,COI0425,0,30,0,"300,00",zrealizowana,
        2025-04-25,zamiana obligacji,COI0425,0,30,3,"300,00",zrealizowana,
    """.trimIndent()

    @Test
    fun `recognizes an Obligacje Skarbowe export by its header`() {
        assertTrue(looksLikeObligacjeSkarboweExport(sampleCsv))
    }

    @Test
    fun `does not recognize BOSSA or the app's own schema`() {
        assertTrue(!looksLikeObligacjeSkarboweExport("data;papier;isin;ilosc;-;cena;wartosc;prowizja;po prowizji\n"))
        assertTrue(!looksLikeObligacjeSkarboweExport("account,ticker,name,type,date,quantity,price\n"))
    }

    @Test
    fun `only position-changing rows produce results, everything else is skipped`() {
        val results = parseObligacjeSkarboweCsv(sampleCsv, accountId = 1, accountName = "Obligacje Skarbowe")
        // BUY COI1223, BUY COI1224, SELL COI1224 (early redemption), SELL COI1223 (rollover), BUY
        // COI1227, SELL COI0425 (rollover) = 6. The "dyspozycja zakupu" duplicate order rows, accrual
        // notices, the zero-quantity side of each pair, the net-zero fee/interest pair, the unexplained
        // "opłata - interwencyjny wykup" fee, "zakup papierów - zamiana", and redemption interest
        // (this source has no cash-operation column at all) are all dropped.
        assertEquals(6, results.size)
        results.forEach { assertTrue(it is CsvRowParseResult.Parsed) }
    }

    @Test
    fun `ignoreCashCheck on every row defaults to false and follows ignoreAccountBalance`() {
        val default = parseObligacjeSkarboweCsv(sampleCsv, accountId = 1, accountName = "Obligacje Skarbowe")
        default.forEach { assertTrue(!(it as CsvRowParseResult.Parsed).row.ignoreCashCheck) }

        val ignoring = parseObligacjeSkarboweCsv(
            sampleCsv,
            accountId = 1,
            accountName = "Obligacje Skarbowe",
            ignoreAccountBalance = true
        )
        ignoring.forEach { assertTrue((it as CsvRowParseResult.Parsed).row.ignoreCashCheck) }
    }

    @Test
    fun `a completed purchase becomes a BUY, its duplicate order row is dropped`() {
        val results = parseObligacjeSkarboweCsv(sampleCsv, accountId = 1, accountName = "Obligacje Skarbowe")
        val buys = results.filterIsInstance<CsvRowParseResult.Parsed>().map { it.row }
            .filter { it.type == InvestmentTransactionType.BUY }
        val coi1223 = buys.single { it.ticker == "COI1223" }
        assertEquals(LocalDate.of(2019, 12, 31), coi1223.date)
        assertEquals(0, BigDecimal("142").compareTo(coi1223.quantity))
        assertEquals(0, BigDecimal("100").compareTo(coi1223.price))
        assertEquals(InvestmentCategory.BOND, coi1223.category)
    }

    @Test
    fun `early redemption combines the order row's quantity with the confirmation row's amount`() {
        val results = parseObligacjeSkarboweCsv(sampleCsv, accountId = 1, accountName = "Obligacje Skarbowe")
        val sell = results.filterIsInstance<CsvRowParseResult.Parsed>().map { it.row }
            .single { it.ticker == "COI1224" && it.type == InvestmentTransactionType.SELL }
        assertEquals(LocalDate.of(2021, 3, 3), sell.date)
        assertEquals(0, BigDecimal("1").compareTo(sell.quantity))
        assertEquals(0, BigDecimal("100").compareTo(sell.price))
    }

    @Test
    fun `rollover is a SELL of the full position regardless of which paired row carries the quantity`() {
        val results = parseObligacjeSkarboweCsv(sampleCsv, accountId = 1, accountName = "Obligacje Skarbowe")
        val sells = results.filterIsInstance<CsvRowParseResult.Parsed>().map { it.row }
            .filter { it.type == InvestmentTransactionType.SELL }

        val coi1223 = sells.single { it.ticker == "COI1223" }
        assertEquals(0, BigDecimal("142").compareTo(coi1223.quantity))
        assertEquals(0, BigDecimal("14200").compareTo(coi1223.quantity * coi1223.price))

        // Same event type, but here the real quantity sits on the *second* of the paired rows.
        val coi0425 = sells.single { it.ticker == "COI0425" }
        assertEquals(0, BigDecimal("3").compareTo(coi0425.quantity))
        assertEquals(0, BigDecimal("100").compareTo(coi0425.price))
    }

    @Test
    fun `the new bond bought via rollover is a plain BUY on its own ticker`() {
        val results = parseObligacjeSkarboweCsv(sampleCsv, accountId = 1, accountName = "Obligacje Skarbowe")
        val buy = results.filterIsInstance<CsvRowParseResult.Parsed>().map { it.row }
            .single { it.ticker == "COI1227" }
        assertEquals(InvestmentTransactionType.BUY, buy.type)
        assertEquals(LocalDate.of(2023, 12, 31), buy.date)
        assertEquals(0, BigDecimal("169").compareTo(buy.quantity))
    }

    @Test
    fun `every row is assigned the caller's chosen account`() {
        val results = parseObligacjeSkarboweCsv(sampleCsv, accountId = 99, accountName = "My IKE bonds")
        results.forEach { result ->
            when (result) {
                is CsvRowParseResult.Parsed -> {
                    assertEquals(99L, result.row.accountId)
                    assertEquals("My IKE bonds", result.row.accountName)
                }
                is CsvRowParseResult.ParsedCashOperation -> {
                    assertEquals(99L, result.row.accountId)
                    assertEquals("My IKE bonds", result.row.accountName)
                }
                is CsvRowParseResult.Invalid -> Unit
            }
        }
    }

    @Test
    fun `an early redemption confirmation with no matching order row is rejected`() {
        val csv = "DATA DYSPOZYCJI,RODZAJ DYSPOZYCJI,KOD OBLIGACJI,NR ZAPISU,SERIA,LICZBA OBLIGACJI,KWOTA OPERACJI,STATUS,UWAGI\n" +
            "2021-03-03,przedterminowy wykup,COI9999,0,31,0,\"100,00\",zrealizowana,\n"
        val results = parseObligacjeSkarboweCsv(csv, accountId = 1, accountName = "Obligacje Skarbowe")
        val error = results.single() as CsvRowParseResult.Invalid
        assertEquals(2, error.rowNumber)
    }

    @Test
    fun `an unparseable date on a row that would otherwise import is rejected`() {
        val csv = "DATA DYSPOZYCJI,RODZAJ DYSPOZYCJI,KOD OBLIGACJI,NR ZAPISU,SERIA,LICZBA OBLIGACJI,KWOTA OPERACJI,STATUS,UWAGI\n" +
            "not-a-date,zakup papierów,COI1223,10803,31,142,\"14 200,00\",zrealizowana,\n"
        val results = parseObligacjeSkarboweCsv(csv, accountId = 1, accountName = "Obligacje Skarbowe")
        val error = results.single() as CsvRowParseResult.Invalid
        assertEquals(2, error.rowNumber)
    }

    @Test
    fun `parsePolishDecimal strips space thousands separators`() {
        assertEquals(0, BigDecimal("14200.00").compareTo(parsePolishDecimal("14 200,00")))
        assertEquals(0, BigDecimal("2719.30").compareTo(parsePolishDecimal("2 719,30")))
        assertNull(parsePolishDecimal(""))
    }
}
