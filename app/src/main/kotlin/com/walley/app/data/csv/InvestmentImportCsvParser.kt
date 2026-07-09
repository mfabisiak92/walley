package com.walley.app.data.csv

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.CsvRowParseResult
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.ParsedImportRow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

private val REQUIRED_HEADERS = listOf("account", "ticker", "name", "type", "date", "quantity", "price")

/**
 * Parses a buy/sell CSV export against the app's schema: one row per event, `account` matched by
 * name against an existing investment account, `category`/`commission` optional. Doesn't apply any
 * cross-row business rules (holdings, cash) — see [com.walley.app.domain.model.validateImportRows]
 * for that; this stage only checks that each row's own fields are well-formed.
 */
fun parseInvestmentImportCsv(text: String, accounts: List<Account>): List<CsvRowParseResult> {
    val lines = parseCsvLines(text)
    if (lines.isEmpty()) return emptyList()

    val header = lines.first().map { it.trim().lowercase() }
    val missing = REQUIRED_HEADERS.filter { it !in header }
    if (missing.isNotEmpty()) {
        return listOf(CsvRowParseResult.Invalid(1, "Missing required column(s): ${missing.joinToString(", ")}"))
    }
    val columnIndex = header.withIndex().associate { (i, name) -> name to i }
    val accountsByName = accounts
        .filter { it.type == AccountType.INVESTMENT }
        .associateBy { it.name.trim().lowercase() }

    return lines.drop(1).mapIndexed { index, fields ->
        val rowNumber = index + 2

        fun field(name: String): String = columnIndex[name]?.let { fields.getOrNull(it) }?.trim().orEmpty()

        val accountName = field("account")
        val account = accountsByName[accountName.lowercase()]
            ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Account \"$accountName\" not found (must be an investment account)")

        val ticker = field("ticker").uppercase()
        if (ticker.isBlank()) return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Ticker is required")

        val name = field("name")
        if (name.isBlank()) return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Name is required")

        val categoryText = field("category")
        val category = if (categoryText.isBlank()) {
            InvestmentCategory.STOCK
        } else {
            InvestmentCategory.entries.find { it.name.equals(categoryText, ignoreCase = true) }
                ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Unknown category \"$categoryText\"")
        }

        val typeText = field("type")
        val type = InvestmentTransactionType.entries.find { it.name.equals(typeText, ignoreCase = true) }
            ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Type must be BUY or SELL, got \"$typeText\"")

        val dateText = field("date")
        val date = try {
            LocalDate.parse(dateText)
        } catch (e: DateTimeParseException) {
            return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Date must be YYYY-MM-DD, got \"$dateText\"")
        }

        val quantity = field("quantity").toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
            ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Quantity must be a positive number")

        val price = field("price").toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
            ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Price must be a positive number")

        val commissionText = field("commission")
        val commission = if (commissionText.isBlank()) {
            BigDecimal.ZERO
        } else {
            commissionText.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
                ?: return@mapIndexed CsvRowParseResult.Invalid(rowNumber, "Commission must be a non-negative number")
        }

        CsvRowParseResult.Parsed(
            ParsedImportRow(
                rowNumber = rowNumber,
                accountId = account.id,
                accountName = account.name,
                ticker = ticker,
                name = name,
                category = category,
                type = type,
                date = date,
                quantity = quantity,
                price = price,
                commission = commission
            )
        )
    }
}
