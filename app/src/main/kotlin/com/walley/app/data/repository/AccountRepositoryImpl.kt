package com.walley.app.data.repository

import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.AccountEntity
import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.InvestmentEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
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

    // Investment accounts' displayed balance is uninvested cash (the stored balance column)
    // plus the current market value of the investments associated with them.
    override fun observeAccounts(): Flow<List<Account>> =
        combine(accountDao.observeAll(), investmentDao.observeAll()) { accounts, investments ->
            accounts
                .map { entity ->
                    val account = entity.toDomain()
                    if (account.type == AccountType.INVESTMENT) {
                        account.copy(balance = account.balance + investmentsValue(entity.id, investments))
                    } else {
                        account
                    }
                }
                .sortedWith(compareBy({ ACCOUNT_TYPE_ORDER[it.type] ?: Int.MAX_VALUE }, { it.name }))
        }

    private fun investmentsValue(accountId: Long, investments: List<InvestmentEntity>): BigDecimal =
        investments
            .filter { it.accountId == accountId }
            .fold(BigDecimal.ZERO) { acc, investment -> acc + investment.quantity * investment.currentPrice }
            .setScale(2, RoundingMode.HALF_UP)

    override suspend fun addAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        initialBalance: BigDecimal,
        taxRate: AccountTaxRate,
        targetAmount: BigDecimal?
    ) {
        accountDao.insert(
            AccountEntity(
                name = name,
                type = type,
                currency = currency,
                balanceMinorUnits = initialBalance.toMinorUnits(),
                taxRate = taxRate,
                targetAmountMinorUnits = targetAmount?.toMinorUnits()
            )
        )
    }

    override suspend fun updateAccount(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        newBalance: BigDecimal,
        targetAmount: BigDecimal?
    ) {
        accountDao.update(accountId, name, type, taxRate, newBalance.toMinorUnits(), targetAmount?.toMinorUnits())
    }

    override suspend fun deleteAccount(accountId: Long) {
        if (investmentDao.countForAccount(accountId) > 0) {
            throw AccountHasLinkedInvestmentsException()
        }
        accountDao.delete(accountId)
    }

    override suspend fun addToBalance(accountId: Long, delta: BigDecimal) {
        accountDao.addToBalance(accountId, delta.toMinorUnits())
    }

    private companion object {
        // Cash first, then checking, then investment and saving at the bottom.
        val ACCOUNT_TYPE_ORDER = mapOf(
            AccountType.CASH to 0,
            AccountType.CHECKING to 1,
            AccountType.INVESTMENT to 2,
            AccountType.SAVING to 3
        )
    }
}
