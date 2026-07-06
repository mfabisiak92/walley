package com.walley.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class Asset(
    val id: Long = 0,
    val name: String,
    val currency: Currency,
    val purchaseValue: BigDecimal,
    val currentValue: BigDecimal,
    val purchaseDate: LocalDate
) {
    val gain: BigDecimal get() = currentValue - purchaseValue

    val gainPercent: BigDecimal? get() =
        if (purchaseValue.signum() == 0) {
            null
        } else {
            (currentValue - purchaseValue).divide(purchaseValue, 4, RoundingMode.HALF_UP) * BigDecimal(100)
        }
}
