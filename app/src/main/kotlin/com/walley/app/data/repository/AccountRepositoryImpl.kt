package com.walley.app.data.repository

import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.AccountEntity
import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.InvestmentEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val investmentDao: InvestmentDao
) : AccountRepository {

    // Investment accounts don't hold a stored balance: their balance is derived
    // from the investments associated with them (quantity × price per unit).
    override fun observeAccounts(): Flow<List<Account>> =
        combine(accountDao.observeAll(), investmentDao.observeAll()) { accounts, investments ->
            accounts.map { entity ->
                val account = entity.toDomain()
                if (account.type == AccountType.INVESTMENT) {
                    account.copy(balance = investmentsValue(entity.id, investments))
                } else {
                    account
                }
            }
        }

    private fun investmentsValue(accountId: Long, investments: List<InvestmentEntity>): BigDecimal =
        investments
            .filter { it.accountId == accountId }
            .fold(BigDecimal.ZERO) { acc, investment -> acc + investment.quantity * investment.price }
            .setScale(2, RoundingMode.HALF_UP)

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
        investmentDao.clearAccountAssociation(accountId)
        accountDao.delete(accountId)
    }
}
