package com.walley.app.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * A deposit, withdrawal, internal transfer, or interest payment applied directly to an
 * [AccountType.INVESTMENT] account's uninvested cash — as opposed to an [InvestmentTransaction], which
 * buys/sells a position. Persisted so a re-imported CSV can recognize an operation it already applied
 * and skip it instead of double-counting.
 */
data class AccountOperation(
    val id: Long = 0,
    val accountId: Long,
    val date: LocalDate,
    val description: String,
    /** Signed: positive increased the account's uninvested cash, negative decreased it. */
    val amount: BigDecimal
)
