package com.walley.app.domain.model

import java.math.BigDecimal

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CHECKING,
    val currency: Currency,
    val balance: BigDecimal
)
