package com.walley.app.data.csv

import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.ParsedImportRow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val BOSSA_COLUMN_COUNT = 9
private val BOSSA_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

/**
 * True if [text] looks like a BOSSA brokerage "historia operacji" export: semicolon-delimited,
 * a fixed 9 columns, first column literally `data`. The header's Polish diacritics (ilość, wartość)
 * aren't checked, since they're prone to mangling depending on the file's original encoding.
 */
fun looksLikeBossaExport(text: String): Boolean {
    val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
    val fields = firstLine.split(';')
    return fields.size == BOSSA_COLUMN_COUNT && fields.first().trim().equals("data", ignoreCase = true)
}

/**
 * Parses a BOSSA export: `data;papier;isin;ilość;-;cena;wartość;prowizja;po prowizji`, one row per
 * buy/sell (`K`/`S`). Columns are matched by fixed position, not by name, for the same encoding
 * reason as [looksLikeBossaExport]. `wartość` and `po prowizji` aren't read since they're derivable
 * from quantity/price/commission. There's no account column — every row is assigned to
 * [accountId]/[accountName], since the whole file is one account's history and the caller has
 * already asked the user which account that is.
 */
fun parseBossaExportCsv(text: String, accountId: Long, accountName: String): List<CsvRowParseResult> {
    val lines = parseCsvLines(text, delimiter = ';')
    if (lines.size <= 1) return emptyList()

    return lines.drop(1).mapIndexed { index, fields ->
        val rowNumber = index + 2

        if (fields.size < BOSSA_COLUMN_COUNT) {
            return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Expected $BOSSA_COLUMN_COUNT columns, found ${fields.size}")
        }

        val dateText = fields[0].trim()
        val date = try {
            LocalDateTime.parse(dateText, BOSSA_DATE_FORMATTER).toLocalDate()
        } catch (e: DateTimeParseException) {
            return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Couldn't parse date \"$dateText\"")
        }

        val name = fields[1].trim()
        if (name.isBlank()) return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Missing security name")

        val ticker = fields[2].trim().uppercase()
        if (ticker.isBlank()) return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Missing ISIN")

        val quantity = parsePolishDecimal(fields[3])?.takeIf { it.signum() > 0 }
            ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Invalid quantity \"${fields[3]}\"")

        val typeText = fields[4].trim().uppercase()
        val type = when (typeText) {
            "K" -> InvestmentTransactionType.BUY
            "S" -> InvestmentTransactionType.SELL
            else -> return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Expected K (buy) or S (sell), got \"$typeText\"")
        }

        val price = parsePolishDecimal(fields[5])?.takeIf { it.signum() > 0 }
            ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Invalid price \"${fields[5]}\"")

        val commission = parsePolishDecimal(fields[7])?.takeIf { it.signum() >= 0 }
            ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Invalid commission \"${fields[7]}\"")

        CsvRowParseResult.Parsed(
            ParsedImportRow(
                rowNumber = rowNumber,
                accountId = accountId,
                accountName = accountName,
                ticker = ticker,
                name = name,
                category = InvestmentCategory.STOCK,
                type = type,
                date = date,
                quantity = quantity,
                price = price,
                commission = commission
            )
        )
    }
}
