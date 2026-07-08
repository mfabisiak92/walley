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
    for (account in accounts) {
        total += convertToCurrency(account.netWorthValue, account.currency, targetCurrency, rates) ?: return null
    }
    for (asset in assets) {
        total += convertToCurrency(asset.currentValue, asset.currency, targetCurrency, rates) ?: return null
    }
    for (liability in liabilities) {
        total -= convertToCurrency(liability.currentBalance, liability.currency, targetCurrency, rates) ?: return null
    }
    return total
}
