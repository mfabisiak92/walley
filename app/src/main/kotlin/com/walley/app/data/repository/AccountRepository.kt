package com.walley.app.data.repository

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun addAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        initialBalance: BigDecimal,
        taxRate: AccountTaxRate,
        targetAmount: BigDecimal?,
        commissionFlat: BigDecimal = BigDecimal.ZERO,
        commissionPercent: BigDecimal = BigDecimal.ZERO
    )
    suspend fun updateAccount(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        newBalance: BigDecimal,
        targetAmount: BigDecimal?,
        commissionFlat: BigDecimal = BigDecimal.ZERO,
        commissionPercent: BigDecimal = BigDecimal.ZERO
    )
    /** @throws AccountHasLinkedInvestmentsException if the account still has linked investments. */
    suspend fun deleteAccount(accountId: Long)
    /** Adds (or subtracts, for a negative delta) an amount to an account's stored balance. */
    suspend fun addToBalance(accountId: Long, delta: BigDecimal)
    /**
     * Overwrites an account's raw stored balance directly, leaving every other field untouched.
     * For an Investment account this sets its **uninvested cash**, same as [updateAccount]'s newBalance.
     */
    suspend fun updateBalance(accountId: Long, newBalance: BigDecimal)
    /** Makes this the one default account; every other account becomes non-default. */
    suspend fun setDefaultAccount(accountId: Long)
}

class AccountHasLinkedInvestmentsException : Exception("Account has linked investments")
