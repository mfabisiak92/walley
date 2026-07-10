package com.walley.app.data.repository

import com.walley.app.domain.model.AccountOperation
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface AccountOperationRepository {
    fun observeAll(): Flow<List<AccountOperation>>
    fun observeForAccount(accountId: Long): Flow<List<AccountOperation>>

    /** Records the operation in the ledger and applies its [amount] to the account's uninvested cash. */
    suspend fun recordAndApply(accountId: Long, date: LocalDate, description: String, amount: BigDecimal)
}
