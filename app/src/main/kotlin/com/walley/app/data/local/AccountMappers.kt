package com.walley.app.data.local

import com.walley.app.domain.model.Account
import java.math.BigDecimal
import java.math.RoundingMode

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    currency = currency,
    balance = BigDecimal(balanceMinorUnits).movePointLeft(2)
)

fun BigDecimal.toMinorUnits(): Long = movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
