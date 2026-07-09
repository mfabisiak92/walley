package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/** A CSV row that parsed cleanly and resolved to a real account — ready for business-rule validation. */
data class ParsedImportRow(
    val rowNumber: Int,
    val accountId: Long,
    val accountName: String,
    val ticker: String,
    val name: String,
    val category: InvestmentCategory,
    val type: InvestmentTransactionType,
    val date: LocalDate,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val commission: BigDecimal
)

/** Result of parsing a single CSV row, before business-rule validation. */
sealed interface CsvRowParseResult {
    data class Parsed(val row: ParsedImportRow) : CsvRowParseResult
    data class Invalid(val rowNumber: Int, val reason: String) : CsvRowParseResult
}

sealed interface ImportRowStatus {
    data object ToImport : ImportRowStatus
    data object Duplicate : ImportRowStatus
    data class Rejected(val reason: String) : ImportRowStatus
}

/** The final fate of one CSV row after both parsing and business-rule validation. [row] is null only for rows that failed to parse at all. */
data class ImportRowOutcome(val rowNumber: Int, val row: ParsedImportRow?, val status: ImportRowStatus)

/**
 * Validates a batch of already-parsed rows against the current portfolio plus each other, replaying
 * them in chronological order and reusing the exact rules manual entry uses — [InvestmentWithTransactions.quantityAvailableOn]
 * for sells, [availableCashToBuy] for buys — by simulating each accepted row into a working copy of
 * the portfolio before checking the next one. This means a rejected buy correctly makes a later sell
 * of the same lot fail too, since the simulated state never includes rejected rows.
 *
 * A row is treated as a duplicate (skipped, not rejected) when an existing or already-accepted
 * transaction for the same account+ticker matches its type, date, quantity, and price exactly.
 */
fun validateImportRows(
    parseResults: List<CsvRowParseResult>,
    accounts: List<Account>,
    investmentsByAccount: Map<Long, List<InvestmentWithTransactions>>
): List<ImportRowOutcome> {
    val accountsById = accounts.associateBy { it.id }
    val working: MutableMap<Long, MutableMap<String, InvestmentWithTransactions>> = investmentsByAccount
        .mapValues { (_, list) -> list.associateBy { it.investment.ticker }.toMutableMap() }
        .toMutableMap()

    val outcomeByRowNumber = mutableMapOf<Int, ImportRowOutcome>()

    val parsedRows = parseResults.filterIsInstance<CsvRowParseResult.Parsed>().map { it.row }
    for (parseResult in parseResults) {
        if (parseResult is CsvRowParseResult.Invalid) {
            outcomeByRowNumber[parseResult.rowNumber] =
                ImportRowOutcome(parseResult.rowNumber, null, ImportRowStatus.Rejected(parseResult.reason))
        }
    }

    for (row in parsedRows.sortedBy { it.date }) {
        val account = accountsById[row.accountId]
        if (account == null) {
            outcomeByRowNumber[row.rowNumber] =
                ImportRowOutcome(row.rowNumber, row, ImportRowStatus.Rejected("Account no longer exists"))
            continue
        }

        val accountInvestments = working.getOrPut(row.accountId) { mutableMapOf() }
        val existingForTicker = accountInvestments[row.ticker]

        val isDuplicate = existingForTicker?.transactions.orEmpty().any { t ->
            t.type == row.type && t.date == row.date &&
                t.quantity.compareTo(row.quantity) == 0 && t.pricePerUnit.compareTo(row.price) == 0
        }
        if (isDuplicate) {
            outcomeByRowNumber[row.rowNumber] = ImportRowOutcome(row.rowNumber, row, ImportRowStatus.Duplicate)
            continue
        }

        val rejection = when (row.type) {
            InvestmentTransactionType.SELL -> {
                val available = existingForTicker?.quantityAvailableOn(row.date) ?: BigDecimal.ZERO
                if (row.quantity > available) {
                    "Only ${available.toPlainString()} ${row.ticker} available to sell as of ${row.date}"
                } else {
                    null
                }
            }
            InvestmentTransactionType.BUY -> {
                val totalCost = (row.quantity * row.price) + row.commission
                val availableCash = availableCashToBuy(account, accountInvestments.values.toList(), row.date)
                if (totalCost > availableCash) {
                    "Only ${availableCash.toPlainString()} ${account.currency.symbol} available in \"${account.name}\" as of ${row.date}"
                } else {
                    null
                }
            }
        }
        if (rejection != null) {
            outcomeByRowNumber[row.rowNumber] = ImportRowOutcome(row.rowNumber, row, ImportRowStatus.Rejected(rejection))
            continue
        }

        val newTransaction = InvestmentTransaction(
            type = row.type,
            date = row.date,
            quantity = row.quantity,
            pricePerUnit = row.price,
            commission = row.commission
        )
        accountInvestments[row.ticker] = if (existingForTicker != null) {
            existingForTicker.copy(transactions = existingForTicker.transactions + newTransaction)
        } else {
            InvestmentWithTransactions(
                investment = Investment(
                    name = row.name,
                    ticker = row.ticker,
                    category = row.category,
                    currency = account.currency,
                    currentPrice = row.price,
                    accountId = row.accountId
                ),
                transactions = listOf(newTransaction)
            )
        }
        outcomeByRowNumber[row.rowNumber] = ImportRowOutcome(row.rowNumber, row, ImportRowStatus.ToImport)
    }

    return parseResults.map { result ->
        val rowNumber = when (result) {
            is CsvRowParseResult.Parsed -> result.row.rowNumber
            is CsvRowParseResult.Invalid -> result.rowNumber
        }
        outcomeByRowNumber.getValue(rowNumber)
    }
}
