package com.walley.app.data.repository

import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.AccountEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Account
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

    override suspend fun addAccount(name: String, currency: Currency, initialBalance: BigDecimal) {
        accountDao.insert(
            AccountEntity(
                name = name,
                currency = currency,
                balanceMinorUnits = initialBalance.toMinorUnits()
            )
        )
    }

    override suspend fun updateBalance(accountId: Long, newBalance: BigDecimal) {
        accountDao.updateBalance(accountId, newBalance.toMinorUnits())
    }
}
