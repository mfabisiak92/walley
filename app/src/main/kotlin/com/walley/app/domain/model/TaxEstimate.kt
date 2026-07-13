package com.walley.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** Combined realized-gain tax estimate across a group of investment accounts. */
data class PortfolioTaxEstimate(
    /** Net realized gain/loss across every taxable account for the year; losses in one account offset gains in another taxed at the same rate. */
    val netRealizedGainLoss: BigDecimal,
    /** Tax owed on [netRealizedGainLoss]; zero when the net is a loss. */
    val taxOwed: BigDecimal
)

/**
 * Estimated tax [this] account alone would owe on its own realized gains in [year], using its own
 * tax rate and ignoring every other account. Null when tax-free, or when the account has a net loss
 * for the year (nothing owed). This is the quick, standalone per-account figure — it doesn't benefit
 * from losses elsewhere, unlike [estimatePortfolioTax].
 */
fun Account.estimatedTaxForYear(investmentsInAccount: List<InvestmentWithTransactions>, year: Int): BigDecimal? {
    if (taxRate.rate.signum() == 0) return null
    val netRealized = investmentsInAccount.fold(BigDecimal.ZERO) { acc, data -> acc + data.realizedGainLossInYear(year) }
    return netRealized.takeIf { it.signum() > 0 }
        ?.multiply(taxRate.rate)
        ?.setScale(2, RoundingMode.HALF_UP)
}

/**
 * Combined tax estimate across every non-tax-free investment account for [year]. Nets realized gains
 * and losses within each tax-rate group — so a loss in one account offsets a gain in another taxed at
 * the same rate — then sums the tax owed per group. Tax-free accounts don't participate at all, since
 * their gains and losses are outside the tax system entirely.
 *
 * [convertToBase] converts an amount in an account's own currency into one common currency, since
 * accounts of the same tax rate can otherwise be in different currencies and can't be netted directly.
 */
fun estimatePortfolioTax(
    accounts: List<Account>,
    investmentsByAccount: Map<Long, List<InvestmentWithTransactions>>,
    year: Int,
    convertToBase: (BigDecimal, Currency) -> BigDecimal = { amount, _ -> amount }
): PortfolioTaxEstimate {
    val taxableAccounts = accounts.filter { it.type == AccountType.INVESTMENT && it.taxRate.rate.signum() > 0 }
    var totalNet = BigDecimal.ZERO
    var totalTax = BigDecimal.ZERO
    taxableAccounts.groupBy { it.taxRate }.forEach { (rate, group) ->
        val netForGroup = group.fold(BigDecimal.ZERO) { acc, account ->
            val accountRealized = investmentsByAccount[account.id].orEmpty()
                .fold(BigDecimal.ZERO) { sum, data -> sum + data.realizedGainLossInYear(year) }
            acc + convertToBase(accountRealized, account.currency)
        }
        totalNet += netForGroup
        if (netForGroup.signum() > 0) {
            totalTax += netForGroup.multiply(rate.rate).setScale(2, RoundingMode.HALF_UP)
        }
    }
    return PortfolioTaxEstimate(netRealizedGainLoss = totalNet, taxOwed = totalTax)
}

/** Total realized gain/loss across [investmentsInAccount] — profit already locked in from every sell ever made in this account, regardless of year. */
fun Account.realizedGain(investmentsInAccount: List<InvestmentWithTransactions>): BigDecimal =
    investmentsInAccount.fold(BigDecimal.ZERO) { acc, data -> acc + data.realizedGainLoss }

/** Tax owed on [realizedGain]; null when tax-free, or when the account has a net realized loss. */
fun Account.realizedGainTax(investmentsInAccount: List<InvestmentWithTransactions>): BigDecimal? {
    if (taxRate.rate.signum() == 0) return null
    return realizedGain(investmentsInAccount).takeIf { it.signum() > 0 }
        ?.multiply(taxRate.rate)
        ?.setScale(2, RoundingMode.HALF_UP)
}

/** [realizedGain] minus [realizedGainTax] (no tax subtracted when none is owed). */
fun Account.netRealizedGain(investmentsInAccount: List<InvestmentWithTransactions>): BigDecimal =
    realizedGain(investmentsInAccount) - (realizedGainTax(investmentsInAccount) ?: BigDecimal.ZERO)

/**
 * Estimated tax owed for every year that has any sell at all, keyed by year — each computed
 * independently from that year's own realized gains via [estimatePortfolioTax]. Years with nothing
 * owed (a net loss, or every account tax-free) are left out entirely.
 */
fun estimatedTaxByYear(
    accounts: List<Account>,
    investmentsByAccount: Map<Long, List<InvestmentWithTransactions>>,
    convertToBase: (BigDecimal, Currency) -> BigDecimal = { amount, _ -> amount }
): Map<Int, BigDecimal> {
    val years = investmentsByAccount.values.asSequence()
        .flatten()
        .flatMap { it.transactions }
        .filter { it.type == InvestmentTransactionType.SELL }
        .map { it.date.year }
        .distinct()
    return years.mapNotNull { year ->
        estimatePortfolioTax(accounts, investmentsByAccount, year, convertToBase).taxOwed
            .takeIf { it.signum() > 0 }
            ?.let { year to it }
    }.toMap()
}
