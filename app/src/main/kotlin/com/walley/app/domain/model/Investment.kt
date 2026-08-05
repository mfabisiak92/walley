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
    val accountId: Long? = null,
    /** Null means the price has never been refreshed (e.g. investments created before this field existed). */
    val lastPriceUpdate: LocalDate? = null,
    /** The Yahoo Finance symbol for this instrument (e.g. "PZU.WA"), set once per investment. Null falls back to [ticker] as-is, which is fine for tickers Yahoo resolves without a suffix (e.g. most US ones). */
    val externalTicker: String? = null,
    /** [currentPrice] as of just before the most recent price update, letting the user revert a price change. Null until the price has been updated at least once. */
    val previousPrice: BigDecimal? = null
)

data class InvestmentTransaction(
    val id: Long = 0,
    val investmentId: Long = 0,
    val type: InvestmentTransactionType,
    val date: LocalDate,
    val quantity: BigDecimal,
    val pricePerUnit: BigDecimal,
    val commission: BigDecimal = BigDecimal.ZERO
) {
    /** Gross trade value before commission: quantity × price. */
    val total: BigDecimal get() = quantity * pricePerUnit

    /** Actual cash impact of this trade after commission: what you paid on a buy, or netted on a sell. */
    val netAmount: BigDecimal get() = when (type) {
        InvestmentTransactionType.BUY -> total + commission
        InvestmentTransactionType.SELL -> total - commission
    }
}

/**
 * An investment's position, derived entirely from its buy/sell events using FIFO cost basis: each
 * sell consumes the oldest open buy lots first, and realizes gain/loss against those lots' cost.
 * This is what lets one ticker bought multiple times stay a single position.
 */
data class InvestmentWithTransactions(
    val investment: Investment,
    /** In reverse-chronological order (most recent first), matching other detail lists in the app. */
    val transactions: List<InvestmentTransaction>
) {
    private val running: RunningState by lazy { runningState(includeRealized = { true }) }

    /** An open buy lot still (partially) held, oldest lots consumed first by sells. */
    private data class Lot(val quantity: BigDecimal, val unitCost: BigDecimal, val purchaseYear: Int)

    /**
     * Replays every transaction in chronological order to track open buy lots, but only sums
     * realized gain/loss for sells where [includeRealized] returns true — lots still get consumed
     * for every sell regardless, since a later sell's cost basis depends on all prior ones.
     */
    private fun runningState(includeRealized: (InvestmentTransaction) -> Boolean): RunningState =
        transactions.sortedWith(compareBy({ it.date }, { it.id }))
            .fold(RunningState(emptyList(), BigDecimal.ZERO, emptyMap())) { state, t ->
                when (t.type) {
                    InvestmentTransactionType.BUY -> {
                        // Commission is folded into cost basis, same as it would show up on a brokerage statement.
                        val unitCost = if (t.quantity.signum() == 0) {
                            BigDecimal.ZERO
                        } else {
                            t.netAmount.divide(t.quantity, 8, RoundingMode.HALF_UP)
                        }
                        state.copy(lots = state.lots + Lot(t.quantity, unitCost, t.date.year))
                    }
                    InvestmentTransactionType.SELL -> {
                        // Commission reduces net proceeds, so it's netted per unit before comparing to cost.
                        val heldQuantity = state.lots.fold(BigDecimal.ZERO) { acc, lot -> acc + lot.quantity }
                        var toSell = t.quantity.min(heldQuantity)
                        val proceedsPerUnit = if (t.quantity.signum() == 0) {
                            BigDecimal.ZERO
                        } else {
                            t.netAmount.divide(t.quantity, 8, RoundingMode.HALF_UP)
                        }

                        var realized = BigDecimal.ZERO
                        val remainingLots = mutableListOf<Lot>()
                        for (lot in state.lots) {
                            if (toSell.signum() <= 0) {
                                remainingLots += lot
                                continue
                            }
                            val consumed = lot.quantity.min(toSell)
                            realized += (proceedsPerUnit - lot.unitCost) * consumed
                            toSell -= consumed
                            val leftover = lot.quantity - consumed
                            if (leftover.signum() > 0) {
                                remainingLots += lot.copy(quantity = leftover)
                            }
                        }

                        state.copy(
                            lots = remainingLots,
                            realizedGainLoss = state.realizedGainLoss +
                                if (includeRealized(t)) realized else BigDecimal.ZERO,
                            realizedByTransactionId = state.realizedByTransactionId + (t.id to realized)
                        )
                    }
                }
            }

    /** Net quantity currently held. */
    val quantity: BigDecimal get() = running.lots.fold(BigDecimal.ZERO) { acc, lot -> acc + lot.quantity }

    /** Weighted average cost per unit across the currently open (oldest-first) buy lots. */
    val averageCost: BigDecimal get() {
        val lots = running.lots
        val totalQuantity = lots.fold(BigDecimal.ZERO) { acc, lot -> acc + lot.quantity }
        if (totalQuantity.signum() == 0) return BigDecimal.ZERO
        val totalCost = lots.fold(BigDecimal.ZERO) { acc, lot -> acc + (lot.quantity * lot.unitCost) }
        return totalCost.divide(totalQuantity, 8, RoundingMode.HALF_UP)
    }

    /** Realized gain/loss from all sells, each matched FIFO against the oldest open buy lots. */
    val realizedGainLoss: BigDecimal get() = running.realizedGainLoss

    /** Realized gain/loss for each individual sell, keyed by transaction id — for showing it per event. */
    val realizedGainLossByTransactionId: Map<Long, BigDecimal> get() = running.realizedByTransactionId

    /** Realized gain/loss from only the sells dated in [year] — what a tax bill for that year is based on. */
    fun realizedGainLossInYear(year: Int): BigDecimal =
        runningState(includeRealized = { it.date.year == year }).realizedGainLoss

    /**
     * Unrealized gain/loss of currently open lots, grouped by the calendar year each lot was bought
     * in. There's no historical market price to value a past year-end by, so this attributes a still-
     * held lot's paper gain to the year the money went in rather than the years it was held — added to
     * [realizedGainLossInYear] across every year, the totals sum to exactly this position's all-time
     * [realizedGainLoss] + [unrealizedGainLoss], just broken down by year instead of realized/unrealized.
     */
    fun unrealizedGainLossByPurchaseYear(): Map<Int, BigDecimal> =
        running.lots
            .groupBy { it.purchaseYear }
            .mapValues { (_, lots) ->
                lots.fold(BigDecimal.ZERO) { acc, lot -> acc + lot.quantity * (investment.currentPrice - lot.unitCost) }
            }

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

    /** Sum of commission paid across every buy/sell event for this position. */
    val totalCommissionPaid: BigDecimal get() = transactions.fold(BigDecimal.ZERO) { acc, t -> acc + t.commission }

    val costBasis: BigDecimal get() = quantity * averageCost
    val currentValue: BigDecimal get() = quantity * investment.currentPrice
    val unrealizedGainLoss: BigDecimal get() = currentValue - costBasis
    val unrealizedGainLossPercent: BigDecimal? get() =
        if (averageCost.signum() == 0) {
            null
        } else {
            (investment.currentPrice - averageCost).divide(averageCost, 4, RoundingMode.HALF_UP) * BigDecimal(100)
        }

    private data class RunningState(
        val lots: List<Lot>,
        val realizedGainLoss: BigDecimal,
        val realizedByTransactionId: Map<Long, BigDecimal>
    )
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
                    InvestmentTransactionType.BUY -> sum + t.netAmount
                    InvestmentTransactionType.SELL -> sum - t.netAmount
                }
            }
        acc + netForInvestment
    }
    return (account.uninvestedCash - netSpent).max(BigDecimal.ZERO)
}
