package com.walley.app.domain.model

import java.math.BigDecimal

enum class AccountTaxRate(val label: String, val rate: BigDecimal) {
    TAX_FREE("Tax-free", BigDecimal.ZERO),
    STANDARD_19("19%", BigDecimal("0.19"))
}
