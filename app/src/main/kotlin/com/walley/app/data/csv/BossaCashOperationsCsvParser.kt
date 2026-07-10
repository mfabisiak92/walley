package com.walley.app.data.csv

import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.ParsedCashOperationRow
import com.walley.app.domain.model.ParsedImportRow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException

// Matches e.g. "PKNORLEN (PLPKN0000018) 34 x 138.82 PLN nr 000000001165" -> ("PKNORLEN", "PLPKN0000018", "34", "138.82").
// The order number is alphanumeric in some exports (e.g. "Z00357144519"), not just digits, so it's
// matched loosely and discarded rather than captured.
private val BOSSA_CASH_OP_TRADE_DETAILS_REGEX =
    Regex("""^(.+?)\s*\(([A-Z0-9]+)\)\s*([\d.]+)\s*x\s*([\d.]+)\s*PLN\s*nr\s*[A-Za-z0-9]+$""")

private const val MIN_COLUMN_COUNT = 4

/**
 * The export comes semicolon-delimited by default, but is easy to end up comma-delimited after a
 * spreadsheet round-trip (with decimal-comma amounts then quoted to protect them, e.g. `"4701,95"`) —
 * so the delimiter is sniffed from whichever character splits the header into at least [MIN_COLUMN_COUNT]
 * columns starting with `data`, rather than assumed. Some exports add extra trailing columns (e.g. a
 * `waluta`/currency column after `kwota`) that this parser doesn't need, so the column count only has a
 * floor, not an exact match — the second column is checked instead to tell this format apart from the
 * dedicated trade export [looksLikeBossaExport] handles, which also starts with `data` but has `papier`
 * (not `tytuł operacji`) second. `tytu` alone is checked since `ł` is prone to encoding mangling.
 */
private fun bossaCashOperationsDelimiter(text: String): Char? {
    val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return null
    return listOf(';', ',').firstOrNull { delimiter ->
        val fields = firstLine.split(delimiter)
        fields.size >= MIN_COLUMN_COUNT &&
            fields[0].trim().equals("data", ignoreCase = true) &&
            fields.getOrNull(1)?.trim()?.startsWith("tytu", ignoreCase = true) == true
    }
}

/**
 * True if [text] looks like a BOSSA (DM BOŚ) "historia operacji pieniężnych" export: at least
 * [MIN_COLUMN_COUNT] columns, first column literally `data` (semicolon- or comma-delimited, see
 * [bossaCashOperationsDelimiter]). This is a different report than the dedicated trade export
 * [looksLikeBossaExport] handles — it's a mixed cash ledger (deposits, dividends, tax, transfers, and
 * trade settlements all in one list), closer in spirit to XTB's Cash Operations export.
 */
fun looksLikeBossaCashOperationsExport(text: String): Boolean = bossaCashOperationsDelimiter(text) != null

/**
 * Parses a BOSSA (DM BOŚ) "historia operacji pieniężnych" export: `data;tytuł operacji;szczegóły;kwota`,
 * plus any extra trailing columns some exports add (e.g. `waluta`), which are ignored. Only rows whose
 * title starts with "Rozliczenie transakcji kupna/sprzedaży" (buy/sell settlement) become trade
 * [ParsedImportRow]s by default; every other row (deposits, dividends, dividend/interest tax,
 * internal/external transfers) is skipped unless [includeAccountOperations] is true, in which case they
 * become [ParsedCashOperationRow]s instead — mirroring [parseXtbCashOperationsCsv].
 *
 * Trade rows don't have a dedicated commission column: quantity and per-share price are read from the
 * free-text `szczegóły` details (e.g. `PKNORLEN (PLPKN0000018) 34 x 138.82 PLN nr 000000001165`), and
 * commission is derived as the difference between that gross trade value and the settled `kwota`
 * amount, which is already net of commission (less than gross on a sell, more negative than gross on
 * a buy). [ParsedImportRow.ticker] is set to the ISIN (not the short name shown in the details), same
 * as [parseBossaExportCsv], so rows from both BOSSA report types dedupe/merge against the same
 * investment instead of creating parallel positions for the same holding.
 */
fun parseBossaCashOperationsCsv(
    text: String,
    accountId: Long,
    accountName: String,
    includeAccountOperations: Boolean = false
): List<CsvRowParseResult> {
    val delimiter = bossaCashOperationsDelimiter(text) ?: ';'
    val lines = parseCsvLines(text, delimiter)
    if (lines.size <= 1) return emptyList()

    val results = mutableListOf<CsvRowParseResult>()
    lines.drop(1).forEachIndexed { index, fields ->
        val rowNumber = index + 2

        if (fields.size < MIN_COLUMN_COUNT) {
            results += CsvRowParseResult.Invalid(rowNumber, "Expected at least $MIN_COLUMN_COUNT columns, found ${fields.size}")
            return@forEachIndexed
        }

        val dateText = fields[0].trim()
        val date = try {
            LocalDate.parse(dateText)
        } catch (e: DateTimeParseException) {
            results += CsvRowParseResult.Invalid(rowNumber, "Couldn't parse date \"$dateText\"")
            return@forEachIndexed
        }

        val title = fields[1].trim()
        val details = fields[2].trim()
        val amountText = fields[3].trim()
        val amount = parsePolishDecimal(amountText)
        if (amount == null) {
            results += CsvRowParseResult.Invalid(rowNumber, "Invalid amount \"$amountText\"")
            return@forEachIndexed
        }

        val isBuy = title.startsWith("Rozliczenie transakcji kupna", ignoreCase = true)
        val isSell = title.startsWith("Rozliczenie transakcji sprzeda", ignoreCase = true)
        if (!isBuy && !isSell) {
            if (includeAccountOperations && title.isNotBlank()) {
                results += CsvRowParseResult.ParsedCashOperation(
                    ParsedCashOperationRow(
                        rowNumber = rowNumber,
                        accountId = accountId,
                        accountName = accountName,
                        date = date,
                        description = title,
                        amount = amount
                    )
                )
            }
            return@forEachIndexed
        }

        val match = BOSSA_CASH_OP_TRADE_DETAILS_REGEX.find(details)
        if (match == null) {
            results += CsvRowParseResult.Invalid(rowNumber, "Couldn't parse trade details \"$details\"")
            return@forEachIndexed
        }
        val (shortName, isin, quantityText, priceText) = match.destructured
        val quantity = quantityText.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        val price = priceText.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        if (quantity == null || price == null) {
            results += CsvRowParseResult.Invalid(rowNumber, "Invalid quantity/price in \"$details\"")
            return@forEachIndexed
        }

        val grossValue = quantity * price
        val commission = (if (isBuy) amount.abs() - grossValue else grossValue - amount)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)

        results += CsvRowParseResult.Parsed(
            ParsedImportRow(
                rowNumber = rowNumber,
                accountId = accountId,
                accountName = accountName,
                ticker = isin.uppercase(),
                name = shortName,
                category = InvestmentCategory.STOCK,
                type = if (isBuy) InvestmentTransactionType.BUY else InvestmentTransactionType.SELL,
                date = date,
                quantity = quantity,
                price = price,
                commission = commission
            )
        )
    }
    return results
}
