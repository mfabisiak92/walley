package com.walley.app.data.repository

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun addAccount(name: String, currency: Currency, initialBalance: BigDecimal)
    suspend fun updateBalance(accountId: Long, newBalance: BigDecimal)
}
