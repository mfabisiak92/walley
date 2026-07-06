package com.walley.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType = AccountType.CHECKING,
    val currency: Currency,
    val balance: BigDecimal,
    val taxRate: AccountTaxRate = AccountTaxRate.STANDARD_19,
    /** Optional savings goal; only meaningful for [AccountType.SAVING] accounts. */
    val targetAmount: BigDecimal? = null,
    /**
     * Cash held in an [AccountType.INVESTMENT] account that hasn't been put into a position yet.
     * [balance] for investment accounts is this value plus the current value of linked investments.
     */
    val uninvestedCash: BigDecimal = BigDecimal.ZERO
) {
    val targetProgressPercent: BigDecimal?
        get() = targetAmount?.takeIf { it.signum() > 0 }
            ?.let { target -> balance.divide(target, 4, RoundingMode.HALF_UP) * BigDecimal(100) }

    val targetReached: Boolean
        get() = targetAmount != null && targetAmount.signum() > 0 && balance >= targetAmount
}
