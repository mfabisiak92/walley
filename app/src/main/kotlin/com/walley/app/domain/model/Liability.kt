package com.walley.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class Liability(
    val id: Long = 0,
    val name: String,
    val currency: Currency,
    val originalAmount: BigDecimal,
    val currentBalance: BigDecimal,
    val startDate: LocalDate
) {
    val paidOffAmount: BigDecimal get() = originalAmount - currentBalance

    val paidOffPercent: BigDecimal? get() =
        if (originalAmount.signum() <= 0) {
            null
        } else {
            (paidOffAmount.divide(originalAmount, 4, RoundingMode.HALF_UP) * BigDecimal(100))
                .coerceIn(BigDecimal.ZERO, BigDecimal(100))
        }
}
