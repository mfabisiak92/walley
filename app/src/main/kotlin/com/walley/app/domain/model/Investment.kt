package com.walley.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

enum class InvestmentTransactionType(val label: String) {
    BUY("Buy"),
    SELL("Sell")
}

data class Investment(
    val id: Long = 0,
    val name: String,
    val ticker: String,
    val category: InvestmentCategory,
    val currency: Currency,
    val currentPrice: BigDecimal,
    val accountId: Long? = null
)

data class InvestmentTransaction(
    val id: Long = 0,
    val investmentId: Long = 0,
    val type: InvestmentTransactionType,
    val date: LocalDate,
    val quantity: BigDecimal,
    val pricePerUnit: BigDecimal
) {
    val total: BigDecimal get() = quantity * pricePerUnit
}

/**
 * An investment's position, derived entirely from its buy/sell events using average cost basis:
 * each sell realizes gain/loss against the running average buy price, matching how most brokerages
 * report it. This is what lets one ticker bought multiple times stay a single position.
 */
data class InvestmentWithTransactions(
    val investment: Investment,
    /** In reverse-chronological order (most recent first), matching other detail lists in the app. */
    val transactions: List<InvestmentTransaction>
) {
    private val running: RunningState by lazy {
        transactions.sortedWith(compareBy({ it.date }, { it.id }))
            .fold(RunningState(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)) { state, t ->
                when (t.type) {
                    InvestmentTransactionType.BUY -> {
                        val newQuantity = state.quantity + t.quantity
                        val newAverageCost = if (newQuantity.signum() == 0) {
                            BigDecimal.ZERO
                        } else {
                            ((state.averageCost * state.quantity) + (t.pricePerUnit * t.quantity))
                                .divide(newQuantity, 8, RoundingMode.HALF_UP)
                        }
                        state.copy(quantity = newQuantity, averageCost = newAverageCost)
                    }
                    InvestmentTransactionType.SELL -> {
                        val soldQuantity = t.quantity.min(state.quantity)
                        val realized = (t.pricePerUnit - state.averageCost) * soldQuantity
                        state.copy(
                            quantity = state.quantity - soldQuantity,
                            realizedGainLoss = state.realizedGainLoss + realized
                        )
                    }
                }
            }
    }

    /** Net quantity currently held. */
    val quantity: BigDecimal get() = running.quantity

    /** Weighted average cost per unit of the currently held quantity. */
    val averageCost: BigDecimal get() = running.averageCost

    /** Realized gain/loss from all sells, against the average cost at the time of each sale. */
    val realizedGainLoss: BigDecimal get() = running.realizedGainLoss

    val firstPurchaseDate: LocalDate? get() =
        transactions.filter { it.type == InvestmentTransactionType.BUY }.minByOrNull { it.date }?.date

    /**
     * Quantity held as of [date] (inclusive), used to stop a sell from exceeding what was actually
     * owned at that point in time. [excludingTransactionId] leaves out the transaction being edited
     * so it doesn't count against itself.
     */
    fun quantityAvailableOn(date: LocalDate, excludingTransactionId: Long? = null): BigDecimal =
        transactions
            .filter { it.id != excludingTransactionId && !it.date.isAfter(date) }
            .sortedWith(compareBy({ it.date }, { it.id }))
            .fold(BigDecimal.ZERO) { qty, t ->
                when (t.type) {
                    InvestmentTransactionType.BUY -> qty + t.quantity
                    InvestmentTransactionType.SELL -> qty - t.quantity.min(qty)
                }
            }

    val costBasis: BigDecimal get() = quantity * averageCost
    val currentValue: BigDecimal get() = quantity * investment.currentPrice
    val unrealizedGainLoss: BigDecimal get() = currentValue - costBasis
    val unrealizedGainLossPercent: BigDecimal? get() =
        if (averageCost.signum() == 0) {
            null
        } else {
            (investment.currentPrice - averageCost).divide(averageCost, 4, RoundingMode.HALF_UP) * BigDecimal(100)
        }

    private data class RunningState(val quantity: BigDecimal, val averageCost: BigDecimal, val realizedGainLoss: BigDecimal)
}

/**
 * Cash left in [account]'s uninvested balance to spend on a new buy as of [date], after netting out
 * every buy/sell dated on or before it across [investmentsInAccount] — all the investments linked to
 * that account, which share its one pool of cash. [excludingTransactionId] leaves out the transaction
 * being edited so it doesn't count against itself. Investments linked to an account are always in that
 * account's currency, so no currency conversion is needed here.
 */
fun availableCashToBuy(
    account: Account,
    investmentsInAccount: List<InvestmentWithTransactions>,
    date: LocalDate,
    excludingTransactionId: Long? = null
): BigDecimal {
    val netSpent = investmentsInAccount.fold(BigDecimal.ZERO) { acc, data ->
        val netForInvestment = data.transactions
            .filter { it.id != excludingTransactionId && !it.date.isAfter(date) }
            .fold(BigDecimal.ZERO) { sum, t ->
                when (t.type) {
                    InvestmentTransactionType.BUY -> sum + t.total
                    InvestmentTransactionType.SELL -> sum - t.total
                }
            }
        acc + netForInvestment
    }
    return (account.uninvestedCash - netSpent).max(BigDecimal.ZERO)
}
