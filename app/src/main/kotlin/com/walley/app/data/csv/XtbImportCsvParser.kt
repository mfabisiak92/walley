package com.walley.app.data.csv

import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.ParsedImportRow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val XTB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

// Matches e.g. "OPEN BUY 8/8.2228 @ 94.42" or "OPEN BUY 10 @ 50.00" (no fill fraction) -> ("BUY", "8").
// The denominator (total order size) isn't needed: each row is its own fill and is imported as its
// own buy/sell event, which the app's average-cost aggregation already blends correctly.
private val XTB_COMMENT_REGEX = Regex("""(BUY|SELL)\s+([\d.]+)(?:/[\d.]+)?\s*@""", RegexOption.IGNORE_CASE)

/**
 * True if [text] looks like an XTB "Cash Operations" export: its first line is the `Account number`
 * metadata row (the actual `Type,Ticker,...` header shows up several rows later).
 */
fun looksLikeXtbCashOperationsExport(text: String): Boolean {
    val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
    return firstLine.split(',').firstOrNull()?.trim().equals("Account number", ignoreCase = true)
}

/**
 * Parses an XTB "Cash Operations" export. Unlike BOSSA this is a mixed ledger — deposits, transfers,
 * interest, and trades all in one list — so only rows whose `Type` is `Stock purchase`/`Stock sale`
 * become import rows; everything else (including the trailing `Total` row) is silently skipped, not
 * rejected, since it was never a buy/sell event to begin with.
 *
 * There's no dedicated quantity/price/commission column: the fill quantity and direction are parsed
 * out of the free-text `Comment` (e.g. `OPEN BUY 8/8.2228 @ 94.42`), and price is derived as
 * `|Amount| / quantity` rather than trusting the comment's `@ price` figure, since that's in the
 * instrument's own trading currency (USD/GBP/EUR) while `Amount` is already in the account's
 * currency — dividing gives the real price paid per unit in that currency. XTB doesn't break out a
 * separate commission in this report, so it's always 0 here.
 */
fun parseXtbCashOperationsCsv(text: String, accountId: Long, accountName: String): List<CsvRowParseResult> {
    val lines = parseCsvLines(text)
    val headerIndex = lines.indexOfFirst { it.firstOrNull()?.trim().equals("Type", ignoreCase = true) }
    if (headerIndex == -1) {
        return listOf(CsvRowParseResult.Invalid(1, "Couldn't find the \"Type\" header row in this file"))
    }

    val results = mutableListOf<CsvRowParseResult>()
    lines.drop(headerIndex + 1).forEachIndexed { index, fields ->
        val rowNumber = headerIndex + 2 + index

        fun field(i: Int): String = fields.getOrNull(i)?.trim().orEmpty()

        val type = field(0)
        if (!type.equals("Stock purchase", ignoreCase = true) && !type.equals("Stock sale", ignoreCase = true)) {
            return@forEachIndexed
        }

        val ticker = field(1).uppercase()
        if (ticker.isBlank()) {
            results += CsvRowParseResult.Invalid(rowNumber, "Missing ticker")
            return@forEachIndexed
        }

        val timeText = field(3)
        val date = try {
            LocalDateTime.parse(timeText, XTB_DATE_FORMATTER).toLocalDate()
        } catch (e: DateTimeParseException) {
            results += CsvRowParseResult.Invalid(rowNumber, "Couldn't parse date \"$timeText\"")
            return@forEachIndexed
        }

        val amountText = field(4)
        val amount = parsePolishDecimal(amountText)
        if (amount == null) {
            results += CsvRowParseResult.Invalid(rowNumber, "Invalid amount \"$amountText\"")
            return@forEachIndexed
        }

        val comment = field(6)
        val match = XTB_COMMENT_REGEX.find(comment)
        if (match == null) {
            results += CsvRowParseResult.Invalid(rowNumber, "Couldn't find quantity/direction in comment \"$comment\"")
            return@forEachIndexed
        }
        val (directionText, quantityText) = match.destructured
        val transactionType = when (directionText.uppercase()) {
            "BUY" -> InvestmentTransactionType.BUY
            "SELL" -> InvestmentTransactionType.SELL
            else -> null
        }
        if (transactionType == null) {
            results += CsvRowParseResult.Invalid(rowNumber, "Unknown trade direction in comment \"$comment\"")
            return@forEachIndexed
        }
        val quantity = quantityText.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        if (quantity == null) {
            results += CsvRowParseResult.Invalid(rowNumber, "Invalid quantity in comment \"$comment\"")
            return@forEachIndexed
        }
        if (amount.signum() == 0) {
            results += CsvRowParseResult.Invalid(rowNumber, "Amount is zero")
            return@forEachIndexed
        }
        val price = amount.abs().divide(quantity, 8, RoundingMode.HALF_UP)

        val name = field(2).ifBlank { ticker }

        results += CsvRowParseResult.Parsed(
            ParsedImportRow(
                rowNumber = rowNumber,
                accountId = accountId,
                accountName = accountName,
                ticker = ticker,
                name = name,
                category = InvestmentCategory.STOCK,
                type = transactionType,
                date = date,
                quantity = quantity,
                price = price,
                commission = BigDecimal.ZERO
            )
        )
    }
    return results
}
