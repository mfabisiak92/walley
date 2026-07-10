package com.walley.app.feature.home

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.Liability
import com.walley.app.feature.budget.convertToCurrency
import java.math.BigDecimal

/** Total net worth (accounts + assets − liabilities) in [targetCurrency]; null if a needed rate is unavailable. */
fun calculateNetWorth(
    accounts: List<Account>,
    assets: List<Asset>,
    liabilities: List<Liability>,
    targetCurrency: Currency,
    rates: ExchangeRates?
): BigDecimal? {
    var total = BigDecimal.ZERO
    // Virtual accounts' balance already sits in a real account, so they're excluded to avoid double-counting.
    for (account in accounts.filterNot { it.isVirtual }) {
        total += convertToCurrency(account.netWorthValue, account.currency, targetCurrency, rates) ?: return null
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
