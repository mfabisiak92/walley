package com.walley.app.data.local

import com.walley.app.domain.model.Investment

fun InvestmentEntity.toDomain(): Investment = Investment(
    id = id,
    name = name,
    ticker = ticker,
    quantity = quantity,
    currency = currency,
    price = price,
    accountId = accountId
)
