package com.walley.app.feature.accounts

import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.util.Locale

fun formatMoney(amount: BigDecimal, currency: Currency): String =
    String.format(Locale.getDefault(), "%,.2f %s", amount, currency.symbol)
