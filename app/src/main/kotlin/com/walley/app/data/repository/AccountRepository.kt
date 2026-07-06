package com.walley.app.data.repository

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun addAccount(name: String, type: AccountType, currency: Currency, initialBalance: BigDecimal)
    suspend fun updateAccount(accountId: Long, name: String, type: AccountType, newBalance: BigDecimal)
    suspend fun deleteAccount(accountId: Long)
}
