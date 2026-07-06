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
        targetAmount: BigDecimal?
    )
    suspend fun updateAccount(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        newBalance: BigDecimal,
        targetAmount: BigDecimal?
    )
    /** @throws AccountHasLinkedInvestmentsException if the account still has linked investments. */
    suspend fun deleteAccount(accountId: Long)
    /** Adds (or subtracts, for a negative delta) an amount to an account's stored balance. */
    suspend fun addToBalance(accountId: Long, delta: BigDecimal)
}

class AccountHasLinkedInvestmentsException : Exception("Account has linked investments")
