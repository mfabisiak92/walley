package com.walley.app.domain.model

import java.math.BigDecimal

/**
 * True if [operation] looks like a dividend-related cash operation for this investment — a dividend
 * payout isn't currently recorded against a specific investment anywhere (imports like BOSSA only
 * produce an account-level [AccountOperation] with a free-text description, e.g. "Wypłata dywidendy
 * PKNORLEN"), so this looks for a description that mentions a dividend (English "dividend" or the
 * Polish BOSSA wording "dywidend") *and* separately mentions this investment by [Investment.name].
 * Matches both the payout itself and its accompanying withholding-tax row (e.g. "Podatek od odsetek
 * lub dywidendy PKNORLEN") — callers filter by sign to tell those apart. Since this is a text match
 * rather than a structural link, it can miss a differently-worded import or a manually entered
 * deposit, and — in principle — could over-match two real securities whose names overlap. Note BOSSA's
 * tax row wording ("odsetek lub dywidendy" = "interest or dividends") also covers cash-account
 * interest tax, so a net figure built from this match can't distinguish the two if both landed on the
 * same account the same day.
 */
private fun Investment.isDividendOperation(operation: AccountOperation): Boolean {
    val description = operation.description
    return (description.contains("dividend", ignoreCase = true) || description.contains("dywidend", ignoreCase = true)) &&
        description.contains(name, ignoreCase = true)
}

/** Gross dividends received for this investment so far, before any withholding tax — see [isDividendOperation]. */
fun Investment.dividendsPaid(accountOperations: List<AccountOperation>): BigDecimal =
    accountOperations
        .filter { it.amount.signum() > 0 && isDividendOperation(it) }
        .fold(BigDecimal.ZERO) { total, operation -> total + operation.amount }

/**
 * Net dividends actually received for this investment so far, after withholding tax — the gross
 * payout plus its paired (negative) tax operation, both matched by [isDividendOperation]. Always
 * less than or equal to [dividendsPaid] in magnitude; equal to it if no matching tax row was found.
 */
fun Investment.netDividendsPaid(accountOperations: List<AccountOperation>): BigDecimal =
    accountOperations
        .filter { isDividendOperation(it) }
        .fold(BigDecimal.ZERO) { total, operation -> total + operation.amount }
