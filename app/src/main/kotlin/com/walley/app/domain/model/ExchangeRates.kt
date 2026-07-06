package com.walley.app.domain.model

import java.math.BigDecimal

data class ExchangeRates(
    val base: Currency,
    // 1 unit of base = rates[currency] units of that currency
    val rates: Map<Currency, BigDecimal>,
    val date: String
)
