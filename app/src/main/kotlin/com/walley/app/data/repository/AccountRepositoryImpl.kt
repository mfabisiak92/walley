package com.walley.app.data.repository

import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.AccountEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun addAccount(name: String, type: AccountType, currency: Currency, initialBalance: BigDecimal) {
        accountDao.insert(
            AccountEntity(
                name = name,
                type = type,
                currency = currency,
                balanceMinorUnits = initialBalance.toMinorUnits()
            )
        )
    }

    override suspend fun updateAccount(accountId: Long, name: String, type: AccountType, newBalance: BigDecimal) {
        accountDao.update(accountId, name, type, newBalance.toMinorUnits())
    }

    override suspend fun deleteAccount(accountId: Long) {
        accountDao.delete(accountId)
    }
}
