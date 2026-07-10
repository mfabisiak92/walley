package com.walley.app.data.repository

import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.AccountOperationDao
import com.walley.app.data.local.AccountOperationEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.AccountOperation
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountOperationRepositoryImpl @Inject constructor(
    private val accountOperationDao: AccountOperationDao,
    private val accountDao: AccountDao
) : AccountOperationRepository {

    override fun observeAll(): Flow<List<AccountOperation>> =
        accountOperationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeForAccount(accountId: Long): Flow<List<AccountOperation>> =
        accountOperationDao.observeForAccount(accountId).map { list -> list.map { it.toDomain() } }

    override suspend fun recordAndApply(accountId: Long, date: LocalDate, description: String, amount: BigDecimal) {
        accountOperationDao.insert(
            AccountOperationEntity(accountId = accountId, date = date, description = description, amount = amount)
        )
        accountDao.addToBalance(accountId, amount.toMinorUnits())
    }
}
