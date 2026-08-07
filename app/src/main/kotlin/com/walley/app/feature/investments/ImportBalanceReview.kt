package com.walley.app.feature.investments

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.ImportRowOutcome
import com.walley.app.domain.model.ImportRowStatus
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentTransaction
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal

/**
 * One accepted row's incremental effect on its account, replayed in the same order
 * [ImportInvestmentsViewModel.confirmImport] commits them — lets the preview show a running
 * "balance: before → after" per row, not just a final total for the whole import.
 */
data class ImportRowBalanceChange(
    val beforeBalance: BigDecimal,
    val afterBalance: BigDecimal,
    val beforeNetBalance: BigDecimal,
    val afterNetBalance: BigDecimal
) {
    val balanceChanged: Boolean get() = beforeBalance.compareTo(afterBalance) != 0
}

/** Before/after balance summary for one account touched by an import, plus how much cash it added. */
data class ImportAccountSummary(
    val accountId: Long,
    val accountName: String,
    val currencySymbol: String,
    val beforeBalance: BigDecimal,
    val afterBalance: BigDecimal,
    val beforeNetBalance: BigDecimal,
    val afterNetBalance: BigDecimal,
    /** Net signed sum of every accepted cash-operation row for this account — deposits minus withdrawals. */
    val netCashAdded: BigDecimal
)

/** Full balance preview for an import: one summary per touched account, plus a per-row breakdown. */
data class ImportBalanceReview(
    val accountSummaries: List<ImportAccountSummary>,
    val rowChangesByRowNumber: Map<Int, ImportRowBalanceChange>
)

/**
 * Simulates every accepted (ToImport) row from [outcomes] onto the accounts/investments they'll
 * actually be applied to, replaying them in the exact order [ImportInvestmentsViewModel.confirmImport]
 * uses (the outcome list's own order — see [validateImportRows]'s note that this is parse order, not
 * chronological order). This previews each row's, and each touched account's overall, before/after
 * balance and net balance without writing anything. [includeAccountOperations] mirrors the same-named
 * commit-time flag: a trade's cost/proceeds only move the account's cash when it's on, so the preview
 * matches what actually happens on confirm.
 */
fun computeImportBalanceReview(
    outcomes: List<ImportRowOutcome>,
    accounts: List<Account>,
    investmentsByAccount: Map<Long, List<InvestmentWithTransactions>>,
    includeAccountOperations: Boolean
): ImportBalanceReview {
    val accountsById = accounts.associateBy { it.id }
    val toImport = outcomes.filter { it.status is ImportRowStatus.ToImport }
    val touchedAccountIds = toImport.mapNotNull { it.row?.accountId ?: it.cashOperation?.accountId }.distinct()

    val rowChanges = mutableMapOf<Int, ImportRowBalanceChange>()
    val summaries = touchedAccountIds.mapNotNull { accountId ->
        val account = accountsById[accountId] ?: return@mapNotNull null
        val workingInvestments = investmentsByAccount[accountId].orEmpty()
            .associateBy { it.investment.ticker }
            .toMutableMap()
        var workingCash = account.uninvestedCash
        var netCashAdded = BigDecimal.ZERO

        fun snapshot(): Pair<BigDecimal, BigDecimal> {
            val investmentsValue = workingInvestments.values.fold(BigDecimal.ZERO) { acc, inv -> acc + inv.currentValue }
            val costBasis = workingInvestments.values.fold(BigDecimal.ZERO) { acc, inv -> acc + inv.costBasis }
            val balance = workingCash + investmentsValue
            val runningAccount = account.copy(balance = balance, uninvestedCash = workingCash, investmentCostBasis = costBasis)
            return balance to runningAccount.netWorthValue
        }

        val (startBalance, startNetBalance) = snapshot()
        var prevBalance = startBalance
        var prevNetBalance = startNetBalance

        toImport
            .filter { (it.row?.accountId ?: it.cashOperation?.accountId) == accountId }
            .forEach { outcome ->
                val cashOperation = outcome.cashOperation
                val row = outcome.row
                if (cashOperation != null) {
                    workingCash += cashOperation.amount
                    netCashAdded += cashOperation.amount
                } else if (row != null) {
                    val newTransaction = InvestmentTransaction(
                        type = row.type,
                        date = row.date,
                        quantity = row.quantity,
                        pricePerUnit = row.price,
                        commission = row.commission
                    )
                    val existing = workingInvestments[row.ticker]
                    workingInvestments[row.ticker] = if (existing != null) {
                        existing.copy(transactions = existing.transactions + newTransaction)
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
                    if (includeAccountOperations) {
                        val tradeValue = row.quantity * row.price
                        workingCash += when (row.type) {
                            InvestmentTransactionType.BUY -> -(tradeValue + row.commission)
                            InvestmentTransactionType.SELL -> tradeValue - row.commission
                        }
                    }
                }
                val (afterBalance, afterNetBalance) = snapshot()
                rowChanges[outcome.rowNumber] = ImportRowBalanceChange(
                    beforeBalance = prevBalance,
                    afterBalance = afterBalance,
                    beforeNetBalance = prevNetBalance,
                    afterNetBalance = afterNetBalance
                )
                prevBalance = afterBalance
                prevNetBalance = afterNetBalance
            }

        ImportAccountSummary(
            accountId = accountId,
            accountName = account.name,
            currencySymbol = account.currency.symbol,
            beforeBalance = startBalance,
            afterBalance = prevBalance,
            beforeNetBalance = startNetBalance,
            afterNetBalance = prevNetBalance,
            netCashAdded = netCashAdded
        )
    }

    return ImportBalanceReview(summaries, rowChanges)
}
