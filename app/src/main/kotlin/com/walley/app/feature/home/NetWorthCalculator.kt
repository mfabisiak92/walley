package com.walley.app.feature.home

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.Liability
import com.walley.app.feature.budget.convertToCurrency
import java.math.BigDecimal

/**
 * Total net worth (accounts + assets − liabilities) in [targetCurrency]; null if a needed rate is
 * unavailable. [includeSavings] lets Saving accounts be excluded entirely, per the user's setting —
 * see [Account.netWorthContribution] for how that interacts with virtual accounts.
 */
fun calculateNetWorth(
    accounts: List<Account>,
    assets: List<Asset>,
    liabilities: List<Liability>,
    targetCurrency: Currency,
    rates: ExchangeRates?,
    includeSavings: Boolean = true
): BigDecimal? {
    var total = BigDecimal.ZERO
    for (account in accounts) {
        val contribution = account.netWorthContribution(includeSavings)
        if (contribution.signum() != 0) {
            total += convertToCurrency(contribution, account.currency, targetCurrency, rates) ?: return null
        }
    }
    for (asset in assets) {
        total += convertToCurrency(asset.currentValue, asset.currency, targetCurrency, rates) ?: return null
    }
    // A fully paid-off liability owes nothing, so it no longer counts against net worth.
    for (liability in liabilities.filter { it.currentBalance.signum() != 0 }) {
        total -= convertToCurrency(liability.currentBalance, liability.currency, targetCurrency, rates) ?: return null
    }
    return total
}
