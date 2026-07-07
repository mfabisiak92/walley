package com.walley.app.data.local

import com.walley.app.domain.model.Liability
import java.math.BigDecimal

fun LiabilityEntity.toDomain(): Liability = Liability(
    id = id,
    name = name,
    currency = currency,
    originalAmount = BigDecimal(originalAmountMinorUnits).movePointLeft(2),
    currentBalance = BigDecimal(currentBalanceMinorUnits).movePointLeft(2),
    startDate = startDate
)
