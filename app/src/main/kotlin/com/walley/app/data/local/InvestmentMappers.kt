package com.walley.app.data.local

import com.walley.app.domain.model.Investment

fun InvestmentEntity.toDomain(): Investment = Investment(
    id = id,
    name = name,
    ticker = ticker,
    category = category,
    purchaseDate = purchaseDate,
    quantity = quantity,
    currency = currency,
    price = price,
    currentPrice = currentPrice,
    accountId = accountId
)
