package com.walley.app.domain.model

import java.math.BigDecimal

data class Investment(
    val id: Long = 0,
    val name: String,
    val ticker: String,
    val quantity: BigDecimal,
    val currency: Currency,
    val price: BigDecimal
) {
    val totalBuyAmount: BigDecimal get() = quantity * price
}
