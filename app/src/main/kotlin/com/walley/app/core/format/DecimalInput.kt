package com.walley.app.core.format

import java.math.BigDecimal

/**
 * Parses user-typed amounts, accepting both '.' and ',' as the decimal separator — some locales'
 * keyboards (including Polish) produce ',' for the decimal key, but [BigDecimal]'s constructor only
 * accepts '.'.
 */
fun String.toBigDecimalOrNullLenient(): BigDecimal? = replace(',', '.').toBigDecimalOrNull()
