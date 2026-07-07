package com.walley.app.data.local

import com.walley.app.domain.model.Account
import java.math.BigDecimal
import java.math.RoundingMode

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    currency = currency,
    // For investment accounts this stored value is uninvested cash; the repository
    // adds the value of linked investments on top to produce the total balance.
    balance = BigDecimal(balanceMinorUnits).movePointLeft(2),
    taxRate = taxRate,
    targetAmount = targetAmountMinorUnits?.let { BigDecimal(it).movePointLeft(2) },
    uninvestedCash = BigDecimal(balanceMinorUnits).movePointLeft(2),
    isDefault = isDefault
)

fun BigDecimal.toMinorUnits(): Long = movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
