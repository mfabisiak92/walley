package com.walley.app.data.local

import com.walley.app.domain.model.AccountOperation

fun AccountOperationEntity.toDomain(): AccountOperation = AccountOperation(
    id = id,
    accountId = accountId,
    date = date,
    description = description,
    amount = amount
)
