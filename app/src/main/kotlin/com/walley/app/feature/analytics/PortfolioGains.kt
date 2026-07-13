package com.walley.app.feature.analytics

import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.feature.budget.convertToCurrency
import java.math.BigDecimal

data class PortfolioGainsSummary(val realizedGainLoss: BigDecimal, val unrealizedGainLoss: BigDecimal)

/**
 * All-time realized and unrealized gain/loss summed across [investments], converted to
 * [targetCurrency]; null if a needed rate is unavailable. Unlike the per-year realized breakdown
 * already shown elsewhere, this covers every sell ever made plus every currently open position.
 */
fun portfolioGainsSummary(
    investments: List<InvestmentWithTransactions>,
    targetCurrency: Currency,
    rates: ExchangeRates?
): PortfolioGainsSummary? {
    var realized = BigDecimal.ZERO
    var unrealized = BigDecimal.ZERO
    for (position in investments) {
        realized += convertToCurrency(position.realizedGainLoss, position.investment.currency, targetCurrency, rates) ?: return null
        unrealized += convertToCurrency(position.unrealizedGainLoss, position.investment.currency, targetCurrency, rates) ?: return null
    }
    return PortfolioGainsSummary(realized, unrealized)
}
