package com.walley.app.domain.model

import java.math.BigDecimal

data class CurrencyTotal(
    val currency: Currency,
    val total: BigDecimal
)
