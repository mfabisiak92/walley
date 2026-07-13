package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/** Common shape for anything replayed in date order during validation: a trade or a cash operation. */
sealed interface ImportReplayRow {
    val rowNumber: Int
    val accountId: Long
    val date: LocalDate
}

/** A CSV row that parsed cleanly and resolved to a real account — ready for business-rule validation. */
data class ParsedImportRow(
    override val rowNumber: Int,
    override val accountId: Long,
    val accountName: String,
    val ticker: String,
    val name: String,
    val category: InvestmentCategory,
    val type: InvestmentTransactionType,
    override val date: LocalDate,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val commission: BigDecimal,
    /**
     * Skips the "not enough cash" rejection on a BUY. For sources whose cash history isn't tracked
     * in Walley (e.g. a bond ledger with no deposit records), that check would otherwise reject
     * every buy regardless of how the purchase was actually funded.
     */
    val ignoreCashCheck: Boolean = false
) : ImportReplayRow

/**
 * A CSV row that moves money into or out of the account itself — a deposit, withdrawal, internal
 * transfer, or interest payment — rather than buying/selling an investment. Importing it adjusts the
 * account's uninvested cash directly by [amount] instead of creating an investment transaction.
 */
data class ParsedCashOperationRow(
    override val rowNumber: Int,
    override val accountId: Long,
    val accountName: String,
    override val date: LocalDate,
    val description: String,
    /** Signed: positive increases the account's uninvested cash, negative decreases it. */
    val amount: BigDecimal
) : ImportReplayRow

/** Result of parsing a single CSV row, before business-rule validation. */
sealed interface CsvRowParseResult {
    data class Parsed(val row: ParsedImportRow) : CsvRowParseResult
    data class ParsedCashOperation(val row: ParsedCashOperationRow) : CsvRowParseResult
    data class Invalid(val rowNumber: Int, val reason: String) : CsvRowParseResult
}

sealed interface ImportRowStatus {
    data object ToImport : ImportRowStatus
    data object Duplicate : ImportRowStatus
    data class Rejected(val reason: String) : ImportRowStatus
}

/**
 * The final fate of one CSV row after both parsing and business-rule validation. Exactly one of
 * [row]/[cashOperation] is set for a row that parsed successfully; both are null only for rows that
 * failed to parse at all.
 */
data class ImportRowOutcome(
    val rowNumber: Int,
    val row: ParsedImportRow? = null,
    val cashOperation: ParsedCashOperationRow? = null,
    val status: ImportRowStatus
)

/**
 * Validates a batch of already-parsed rows against the current portfolio plus each other, replaying
 * them in chronological order and reusing the exact rules manual entry uses — [InvestmentWithTransactions.quantityAvailableOn]
 * for sells, [availableCashToBuy] for buys — by simulating each accepted row into a working copy of
 * the portfolio before checking the next one. This means a rejected buy correctly makes a later sell
 * of the same lot fail too, since the simulated state never includes rejected rows.
 *
 * Cash operations (deposits, transfers, interest) are replayed in the same chronological pass, before
 * trades on the same date, so a same-day deposit funds a same-day buy — each accepted one bumps a
 * running per-account cash adjustment that [availableCashToBuy] checks are layered on top of the
 * account's stored uninvested cash for every later buy. Like trades, a cash operation is treated as a
 * duplicate (skipped, not rejected, and not counted towards [availableCashToBuy]) when an existing or
 * already-accepted [AccountOperation] for the same account matches its date, description, and amount
 * exactly — [accountOperationsByAccount] is that existing ledger, keyed by account id.
 *
 * A trade row is treated as a duplicate when an existing or already-accepted transaction for the same
 * account+ticker matches its type, date, quantity, and price exactly.
 */
fun validateImportRows(
    parseResults: List<CsvRowParseResult>,
    accounts: List<Account>,
    investmentsByAccount: Map<Long, List<InvestmentWithTransactions>>,
    accountOperationsByAccount: Map<Long, List<AccountOperation>> = emptyMap()
): List<ImportRowOutcome> {
    val accountsById = accounts.associateBy { it.id }
    val working: MutableMap<Long, MutableMap<String, InvestmentWithTransactions>> = investmentsByAccount
        .mapValues { (_, list) -> list.associateBy { it.investment.ticker }.toMutableMap() }
        .toMutableMap()
    val workingOperations: MutableMap<Long, MutableList<AccountOperation>> = accountOperationsByAccount
        .mapValues { (_, list) -> list.toMutableList() }
        .toMutableMap()
    val cashAdjustmentByAccount = mutableMapOf<Long, BigDecimal>()

    val outcomeByRowNumber = mutableMapOf<Int, ImportRowOutcome>()
    for (parseResult in parseResults) {
        if (parseResult is CsvRowParseResult.Invalid) {
            outcomeByRowNumber[parseResult.rowNumber] =
                ImportRowOutcome(parseResult.rowNumber, status = ImportRowStatus.Rejected(parseResult.reason))
        }
    }

    val tradeRows = parseResults.filterIsInstance<CsvRowParseResult.Parsed>().map { it.row }
    val cashOperationRows = parseResults.filterIsInstance<CsvRowParseResult.ParsedCashOperation>().map { it.row }
    val events: List<ImportReplayRow> = (cashOperationRows + tradeRows)
        .sortedWith(compareBy({ it.date }, { it !is ParsedCashOperationRow }))

    for (event in events) when (event) {
        is ParsedCashOperationRow -> {
            if (accountsById[event.accountId] == null) {
                outcomeByRowNumber[event.rowNumber] =
                    ImportRowOutcome(event.rowNumber, cashOperation = event, status = ImportRowStatus.Rejected("Account no longer exists"))
            } else {
                val existingOperations = workingOperations.getOrPut(event.accountId) { mutableListOf() }
                val isDuplicate = existingOperations.any { op ->
                    op.date == event.date && op.description == event.description && op.amount.compareTo(event.amount) == 0
                }
                if (isDuplicate) {
                    outcomeByRowNumber[event.rowNumber] =
                        ImportRowOutcome(event.rowNumber, cashOperation = event, status = ImportRowStatus.Duplicate)
                } else {
                    existingOperations += AccountOperation(
                        accountId = event.accountId,
                        date = event.date,
                        description = event.description,
                        amount = event.amount
                    )
                    cashAdjustmentByAccount[event.accountId] = (cashAdjustmentByAccount[event.accountId] ?: BigDecimal.ZERO) + event.amount
                    outcomeByRowNumber[event.rowNumber] =
                        ImportRowOutcome(event.rowNumber, cashOperation = event, status = ImportRowStatus.ToImport)
                }
            }
        }
        is ParsedImportRow -> {
            val row = event
            val account = accountsById[row.accountId]
            if (account == null) {
                outcomeByRowNumber[row.rowNumber] =
                    ImportRowOutcome(row.rowNumber, row, status = ImportRowStatus.Rejected("Account no longer exists"))
                continue
            }

            val accountInvestments = working.getOrPut(row.accountId) { mutableMapOf() }
            val existingForTicker = accountInvestments[row.ticker]

            val isDuplicate = existingForTicker?.transactions.orEmpty().any { t ->
                t.type == row.type && t.date == row.date &&
                    t.quantity.compareTo(row.quantity) == 0 && t.pricePerUnit.compareTo(row.price) == 0
            }
            if (isDuplicate) {
                outcomeByRowNumber[row.rowNumber] = ImportRowOutcome(row.rowNumber, row, status = ImportRowStatus.Duplicate)
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
                InvestmentTransactionType.BUY -> if (row.ignoreCashCheck) {
                    null
                } else {
                    val totalCost = (row.quantity * row.price) + row.commission
                    val cashAdjustment = cashAdjustmentByAccount[row.accountId] ?: BigDecimal.ZERO
                    val effectiveAccount = if (cashAdjustment.signum() != 0) {
                        account.copy(uninvestedCash = account.uninvestedCash + cashAdjustment)
                    } else {
                        account
                    }
                    val availableCash = availableCashToBuy(effectiveAccount, accountInvestments.values.toList(), row.date)
                    if (totalCost > availableCash) {
                        "Only ${availableCash.toPlainString()} ${account.currency.symbol} available in \"${account.name}\" as of ${row.date}"
                    } else {
                        null
                    }
                }
            }
            if (rejection != null) {
                outcomeByRowNumber[row.rowNumber] = ImportRowOutcome(row.rowNumber, row, status = ImportRowStatus.Rejected(rejection))
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
            outcomeByRowNumber[row.rowNumber] = ImportRowOutcome(row.rowNumber, row, status = ImportRowStatus.ToImport)
        }
    }

    return parseResults.map { result ->
        val rowNumber = when (result) {
            is CsvRowParseResult.Parsed -> result.row.rowNumber
            is CsvRowParseResult.ParsedCashOperation -> result.row.rowNumber
            is CsvRowParseResult.Invalid -> result.rowNumber
        }
        outcomeByRowNumber.getValue(rowNumber)
    }
}
