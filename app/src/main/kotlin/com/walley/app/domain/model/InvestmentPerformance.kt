package com.walley.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

private data class CashFlow(val date: LocalDate, val amount: Double)

/**
 * Money-weighted annualized return (XIRR), as a percentage (e.g. 14.2 means 14.2%/year), computed
 * entirely in this position's own currency — never converted, since only *current* FX rates are
 * cached (no historical rate storage), and a cross-currency XIRR would silently misstate the return.
 *
 * Cash flows: each BUY's [InvestmentTransaction.netAmount] as a negative outflow on its date, each
 * SELL's as a positive inflow on its date, plus — if [InvestmentWithTransactions.quantity] is still
 * positive as of [asOf] — a final synthetic inflow of [InvestmentWithTransactions.currentValue] dated
 * [asOf], as if the remaining position were liquidated today. Null when there are fewer than 2 cash
 * flows, all flows share a sign (no valid root), all flows share one date (no time value to solve
 * for), or the solver can't converge.
 */
fun InvestmentWithTransactions.xirr(asOf: LocalDate = LocalDate.now()): BigDecimal? {
    val flows = transactions.map { t ->
        val amount = when (t.type) {
            InvestmentTransactionType.BUY -> -t.netAmount
            InvestmentTransactionType.SELL -> t.netAmount
        }
        CashFlow(t.date, amount.toDouble())
    } + if (quantity.signum() > 0) listOf(CashFlow(asOf, currentValue.toDouble())) else emptyList()

    val rate = solveXirr(flows) ?: return null
    return BigDecimal.valueOf(rate * 100).setScale(4, RoundingMode.HALF_UP)
}

/**
 * Simple point-to-point annualized return, as a percentage — treats all capital invested as a single
 * lump sum from [InvestmentWithTransactions.firstPurchaseDate] to [asOf], ignoring the timing of
 * interim buys/sells (unlike [xirr]). `beginValue` is the total BUY cost, `endValue` is current value
 * plus everything already returned via SELLs. Null when there's no purchase history, the span is 0
 * days, or `beginValue` isn't positive (no meaningful ratio to take a root of).
 */
fun InvestmentWithTransactions.cagr(asOf: LocalDate = LocalDate.now()): BigDecimal? {
    val start = firstPurchaseDate ?: return null
    val days = ChronoUnit.DAYS.between(start, asOf)
    if (days <= 0) return null

    val beginValue = transactions
        .filter { it.type == InvestmentTransactionType.BUY }
        .fold(BigDecimal.ZERO) { acc, t -> acc + t.netAmount }
    if (beginValue.signum() <= 0) return null

    val sellProceeds = transactions
        .filter { it.type == InvestmentTransactionType.SELL }
        .fold(BigDecimal.ZERO) { acc, t -> acc + t.netAmount }
    val endValue = currentValue + sellProceeds
    if (endValue.signum() <= 0) return null

    val years = days / 365.0
    val result = (endValue.toDouble() / beginValue.toDouble()).pow(1.0 / years) - 1.0
    if (!result.isFinite()) return null
    return BigDecimal.valueOf(result * 100).setScale(4, RoundingMode.HALF_UP)
}

/**
 * Solves `Σ CF_i / (1+r)^t_i = 0` for `r` (t_i in years from the earliest flow), Excel-XIRR
 * convention. Uses Double internally — the one deliberate exception to this codebase's BigDecimal
 * convention, since Newton-Raphson is a converging numerical estimate, not an exact ledger sum, and
 * BigDecimal buys no precision here at the cost of real complexity.
 */
private fun solveXirr(cashFlows: List<CashFlow>): Double? {
    if (cashFlows.size < 2) return null
    val hasPositive = cashFlows.any { it.amount > 0 }
    val hasNegative = cashFlows.any { it.amount < 0 }
    if (!hasPositive || !hasNegative) return null

    val earliestDay = cashFlows.minOf { it.date }.toEpochDay().toDouble()
    val latestDay = cashFlows.maxOf { it.date }.toEpochDay().toDouble()
    if (earliestDay == latestDay) return null

    val years = cashFlows.map { (it.date.toEpochDay().toDouble() - earliestDay) / 365.0 }
    val amounts = cashFlows.map { it.amount }

    fun npv(rate: Double): Double = amounts.indices.sumOf { i -> amounts[i] / (1.0 + rate).pow(years[i]) }
    fun npvDerivative(rate: Double): Double = amounts.indices.sumOf { i ->
        if (years[i] == 0.0) 0.0 else -years[i] * amounts[i] / (1.0 + rate).pow(years[i] + 1.0)
    }

    return newtonRaphson(::npv, ::npvDerivative) ?: bisection(::npv)
}

private fun newtonRaphson(npv: (Double) -> Double, npvDerivative: (Double) -> Double): Double? {
    var rate = 0.1
    for (i in 0 until 50) {
        val value = npv(rate)
        if (abs(value) < 1e-7) return rate
        val derivative = npvDerivative(rate)
        if (derivative == 0.0 || !derivative.isFinite()) return null
        val next = rate - value / derivative
        if (!next.isFinite() || next <= -1.0) return null
        if (abs(next - rate) < 1e-9) return next
        rate = next
    }
    return null
}

private fun bisection(npv: (Double) -> Double): Double? {
    var lo = -0.9999
    var hi = 10.0
    var npvLo = npv(lo)
    var npvHi = npv(hi)

    var widenAttempts = 0
    while (npvLo.sameSignAs(npvHi) && widenAttempts < 10) {
        hi *= 2
        npvHi = npv(hi)
        widenAttempts++
    }
    if (npvLo.sameSignAs(npvHi) || !npvLo.isFinite() || !npvHi.isFinite()) return null

    repeat(100) {
        val mid = (lo + hi) / 2.0
        val npvMid = npv(mid)
        if (abs(npvMid) < 1e-7 || (hi - lo) < 1e-9) {
            lo = mid
            hi = mid
        } else if (npvMid.sameSignAs(npvLo)) {
            lo = mid
            npvLo = npvMid
        } else {
            hi = mid
        }
    }
    return (lo + hi) / 2.0
}

private fun Double.sameSignAs(other: Double): Boolean = (this >= 0.0) == (other >= 0.0)
