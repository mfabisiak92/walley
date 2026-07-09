package com.walley.app.data.local

import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentTransaction

fun InvestmentEntity.toDomain(): Investment = Investment(
    id = id,
    name = name,
    ticker = ticker,
    category = category,
    currency = currency,
    currentPrice = currentPrice,
    accountId = accountId
)

fun InvestmentTransactionEntity.toDomain(): InvestmentTransaction = InvestmentTransaction(
    id = id,
    investmentId = investmentId,
    type = type,
    date = date,
    quantity = quantity,
    pricePerUnit = pricePerUnit,
    commission = commission
)
