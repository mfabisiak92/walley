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
    val uninvestedCash: BigDecimal = BigDecimal.ZERO,
    /** Sum of what was paid for the positions linked to this [AccountType.INVESTMENT] account. */
    val investmentCostBasis: BigDecimal = BigDecimal.ZERO,
    /** Exactly one account is default at a time; the first account created becomes default automatically. */
    val isDefault: Boolean = false,
    /** Flat broker fee charged per buy/sell trade on this [AccountType.INVESTMENT] account. */
    val commissionFlat: BigDecimal = BigDecimal.ZERO,
    /** Broker fee as a whole-number percentage of trade value (e.g. 0.5 means 0.5%). */
    val commissionPercent: BigDecimal = BigDecimal.ZERO,
    /**
     * True if this account doesn't exist in the real world — its balance is really just an
     * earmarked slice of another account's money (e.g. a "Vacation fund" envelope inside Checking).
     * Excluded from net worth and other cross-account totals to avoid double-counting.
     */
    val isVirtual: Boolean = false
) {
    val targetProgressPercent: BigDecimal?
        get() = targetAmount?.takeIf { it.signum() > 0 }
            ?.let { target -> balance.divide(target, 4, RoundingMode.HALF_UP) * BigDecimal(100) }

    val targetReached: Boolean
        get() = targetAmount != null && targetAmount.signum() > 0 && balance >= targetAmount

    /** Current value minus cost basis of linked investments; zero for anything but [AccountType.INVESTMENT]. */
    val investmentGainLoss: BigDecimal
        get() = if (type == AccountType.INVESTMENT) (balance - uninvestedCash) - investmentCostBasis else BigDecimal.ZERO

    /** Tax owed on the gain, if there is a gain and [taxRate] isn't [AccountTaxRate.TAX_FREE]; null otherwise. */
    val investmentTaxAmount: BigDecimal?
        get() = investmentGainLoss.takeIf { it.signum() > 0 && taxRate.rate.signum() > 0 }
            ?.multiply(taxRate.rate)?.setScale(2, RoundingMode.HALF_UP)

    /** Gain after tax; null whenever [investmentTaxAmount] is null. */
    val investmentNetProfit: BigDecimal?
        get() = investmentTaxAmount?.let { investmentGainLoss - it }

    /** [balance] minus tax owed on unrealized investment gains — what this account actually contributes to net worth. */
    val netWorthValue: BigDecimal
        get() = balance - (investmentTaxAmount ?: BigDecimal.ZERO)

    /** Commission for a trade worth [tradeValue] — whichever of [commissionFlat] or [commissionPercent] is higher, matching how many brokers price it. */
    fun defaultCommission(tradeValue: BigDecimal): BigDecimal {
        val percentAmount = (tradeValue * commissionPercent).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return commissionFlat.max(percentAmount)
    }
}
