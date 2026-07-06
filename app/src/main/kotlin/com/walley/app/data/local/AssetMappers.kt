package com.walley.app.data.local

import com.walley.app.domain.model.Asset
import java.math.BigDecimal

fun AssetEntity.toDomain(): Asset = Asset(
    id = id,
    name = name,
    currency = currency,
    purchaseValue = BigDecimal(purchaseValueMinorUnits).movePointLeft(2),
    currentValue = BigDecimal(currentValueMinorUnits).movePointLeft(2),
    purchaseDate = purchaseDate
)
