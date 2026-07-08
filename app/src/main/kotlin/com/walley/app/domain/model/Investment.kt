package com.walley.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class Investment(
    val id: Long = 0,
    val name: String,
    val ticker: String,
    val category: InvestmentCategory,
    val purchaseDate: LocalDate,
    val quantity: BigDecimal,
    val currency: Currency,
    val price: BigDecimal,
    val currentPrice: BigDecimal,
    val accountId: Long? = null
) {
    /** Cost basis: what was paid for the position. */
    val costBasis: BigDecimal get() = quantity * price

    /** Current market value based on the manually-entered current price. */
    val currentValue: BigDecimal get() = quantity * currentPrice

    val gainLoss: BigDecimal get() = currentValue - costBasis

    val gainLossPercent: BigDecimal? get() =
        if (price.signum() == 0) {
            null
        } else {
            (currentPrice - price).divide(price, 4, RoundingMode.HALF_UP) * BigDecimal(100)
        }
}
