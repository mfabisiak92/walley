package com.walley.app.core.format

import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.util.Locale

fun formatMoney(amount: BigDecimal, currency: Currency): String =
    String.format(Locale.getDefault(), "%,.2f %s", amount, currency.symbol)

/** Formats money using a currency symbol string. */
fun formatMoney(amount: BigDecimal, currencySymbol: String): String =
    String.format(Locale.getDefault(), "%,.2f %s", amount, currencySymbol)

/**
 * Whole-currency-unit formatting (no cents) for compact summary displays where the extra ".00" costs
 * more width than it's worth — e.g. a multi-column header where showing cents risks the currency
 * symbol wrapping onto its own line. Not for anywhere precision actually matters.
 */
fun formatMoneyWhole(amount: BigDecimal, currency: Currency): String =
    String.format(Locale.getDefault(), "%,.0f %s", amount, currency.symbol)
