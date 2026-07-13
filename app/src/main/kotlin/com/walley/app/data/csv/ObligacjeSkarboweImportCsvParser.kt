package com.walley.app.data.csv

import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.ParsedImportRow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val OBLIGACJE_SKARBOWE_COLUMN_COUNT = 9

private const val TYPE_BUY_CONFIRMED = "zakup papierów"
private const val TYPE_ROLLOVER = "zamiana obligacji"
private const val TYPE_EARLY_REDEMPTION_ORDER = "dyspozycja przedterminowego wykupu"
private const val TYPE_EARLY_REDEMPTION_CONFIRMED = "przedterminowy wykup"

/**
 * True if [text] looks like an "Obligacje Skarbowe" (obligacjeskarbowe.pl) transaction history
 * export: comma-delimited, a fixed 9 columns, header starting with `DATA DYSPOZYCJI` — plain ASCII,
 * so unlike BOSSA's header this one is safe to match exactly regardless of the file's encoding.
 */
fun looksLikeObligacjeSkarboweExport(text: String): Boolean {
    val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
    val fields = parseCsvLines(firstLine).firstOrNull() ?: return false
    return fields.size == OBLIGACJE_SKARBOWE_COLUMN_COUNT &&
        fields.first().trim().equals("DATA DYSPOZYCJI", ignoreCase = true)
}

/**
 * Parses an "Obligacje Skarbowe" transaction history export into buy/sell events. The source ledger
 * records far more row types than the app models — order vs. confirmation pairs, annual
 * interest-accrual notices, redemption interest, fee adjustments — so only the rows that change a
 * bond position are kept; everything else is silently skipped:
 *
 * - `zakup papierów` (quantity > 0) is a completed purchase -> BUY, ticker/name from `KOD OBLIGACJI`.
 *   Its paired `dyspozycja zakupu` order row carries identical figures and is dropped so the
 *   purchase isn't counted twice.
 * - `zamiana obligacji` is a maturing bond rolled straight into a new one -> SELL of the full
 *   position. It's reported as two rows sharing a date/ticker/amount, one with the real quantity and
 *   one with 0 (the zero one's order isn't consistent, so both are checked and only the nonzero one
 *   is kept).
 * - Early redemption is split across two rows: `dyspozycja przedterminowego wykupu` carries the
 *   quantity (its amount is always 0, not yet settled) and the following `przedterminowy wykup` for
 *   the same ticker carries the settled amount (its quantity is always 0) -> combined into one SELL.
 *
 * Skipped entirely, since the app has no cash-operation column that maps to them and this source
 * doesn't record deposits at all: `naliczenie odsetek na ...`/`naliczenie wykupu na ...` (running
 * accrual notices), `wykup - odsetki` (redemption interest — capitalized straight back into the next
 * bond, not withdrawable), `zakup papierów - zamiana` (an informational duplicate of the rollover
 * purchase already captured by `zakup papierów`; its quantity is always 0), the early-redemption
 * `odsetki`/`opłata za przedterminowy wykup` pair, and the occasional `opłata - interwencyjny wykup`
 * fee.
 *
 * Because redemption interest is never tracked as cash, a rollover's SELL is smaller than the BUY it
 * funds, and this account's cash balance isn't otherwise tracked in Walley (this ledger has no
 * deposit records at all) — so every BUY row is produced with [ParsedImportRow.ignoreCashCheck] set
 * to [ignoreAccountBalance], which tells [com.walley.app.domain.model.validateImportRows] to skip its
 * "not enough cash" rejection. Leave it false to see rejections for buys the file doesn't fully
 * account for.
 */
fun parseObligacjeSkarboweCsv(
    text: String,
    accountId: Long,
    accountName: String,
    ignoreAccountBalance: Boolean = false
): List<CsvRowParseResult> {
    val lines = parseCsvLines(text)
    if (lines.size <= 1) return emptyList()

    val results = mutableListOf<CsvRowParseResult>()
    val pendingEarlyRedemptions = mutableMapOf<String, BigDecimal>()

    lines.drop(1).forEachIndexed { index, fields ->
        val rowNumber = index + 2

        if (fields.size < OBLIGACJE_SKARBOWE_COLUMN_COUNT) {
            results += CsvRowParseResult.Invalid(rowNumber, "Expected $OBLIGACJE_SKARBOWE_COLUMN_COUNT columns, found ${fields.size}")
            return@forEachIndexed
        }

        fun field(i: Int): String = fields[i].trim()

        val dateText = field(0)
        val type = field(1)
        val ticker = field(2).uppercase()
        val quantity = parsePolishDecimal(field(5))
        val amount = parsePolishDecimal(field(6))

        when {
            type.equals(TYPE_EARLY_REDEMPTION_ORDER, ignoreCase = true) -> {
                val qty = quantity?.takeIf { it.signum() > 0 }
                if (qty != null) pendingEarlyRedemptions[ticker] = qty
            }

            type.equals(TYPE_EARLY_REDEMPTION_CONFIRMED, ignoreCase = true) -> {
                val qty = pendingEarlyRedemptions.remove(ticker)
                if (qty == null) {
                    results += CsvRowParseResult.Invalid(rowNumber, "Early redemption confirmation for \"$ticker\" with no matching order row")
                    return@forEachIndexed
                }
                val date = parseObligacjeSkarboweDate(dateText)
                if (date == null) {
                    results += CsvRowParseResult.Invalid(rowNumber, "Couldn't parse date \"$dateText\"")
                    return@forEachIndexed
                }
                val amt = amount?.takeIf { it.signum() > 0 }
                if (amt == null) {
                    results += CsvRowParseResult.Invalid(rowNumber, "Invalid amount \"${field(6)}\"")
                    return@forEachIndexed
                }
                results += tradeRow(rowNumber, accountId, accountName, ticker, InvestmentTransactionType.SELL, date, qty, amt, ignoreAccountBalance)
            }

            type.equals(TYPE_BUY_CONFIRMED, ignoreCase = true) -> {
                val qty = quantity?.takeIf { it.signum() > 0 } ?: return@forEachIndexed
                val date = parseObligacjeSkarboweDate(dateText)
                if (date == null) {
                    results += CsvRowParseResult.Invalid(rowNumber, "Couldn't parse date \"$dateText\"")
                    return@forEachIndexed
                }
                val amt = amount?.takeIf { it.signum() > 0 }
                if (amt == null) {
                    results += CsvRowParseResult.Invalid(rowNumber, "Invalid amount \"${field(6)}\"")
                    return@forEachIndexed
                }
                results += tradeRow(rowNumber, accountId, accountName, ticker, InvestmentTransactionType.BUY, date, qty, amt, ignoreAccountBalance)
            }

            type.equals(TYPE_ROLLOVER, ignoreCase = true) -> {
                val qty = quantity?.takeIf { it.signum() > 0 } ?: return@forEachIndexed
                val date = parseObligacjeSkarboweDate(dateText)
                if (date == null) {
                    results += CsvRowParseResult.Invalid(rowNumber, "Couldn't parse date \"$dateText\"")
                    return@forEachIndexed
                }
                val amt = amount?.takeIf { it.signum() > 0 }
                if (amt == null) {
                    results += CsvRowParseResult.Invalid(rowNumber, "Invalid amount \"${field(6)}\"")
                    return@forEachIndexed
                }
                results += tradeRow(rowNumber, accountId, accountName, ticker, InvestmentTransactionType.SELL, date, qty, amt, ignoreAccountBalance)
            }

            else -> Unit
        }
    }
    return results
}

private fun parseObligacjeSkarboweDate(text: String): LocalDate? = try {
    LocalDate.parse(text)
} catch (e: DateTimeParseException) {
    null
}

private fun tradeRow(
    rowNumber: Int,
    accountId: Long,
    accountName: String,
    ticker: String,
    type: InvestmentTransactionType,
    date: LocalDate,
    quantity: BigDecimal,
    amount: BigDecimal,
    ignoreAccountBalance: Boolean
): CsvRowParseResult.Parsed = CsvRowParseResult.Parsed(
    ParsedImportRow(
        rowNumber = rowNumber,
        accountId = accountId,
        accountName = accountName,
        ticker = ticker,
        name = ticker,
        category = InvestmentCategory.BOND,
        type = type,
        date = date,
        quantity = quantity,
        price = amount.divide(quantity, 8, RoundingMode.HALF_UP),
        commission = BigDecimal.ZERO,
        ignoreCashCheck = ignoreAccountBalance
    )
)
