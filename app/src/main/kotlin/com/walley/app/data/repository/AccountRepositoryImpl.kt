package com.walley.app.data.repository

import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.AccountEntity
import com.walley.app.data.local.AccountOperationDao
import com.walley.app.data.local.AdHocBudgetDao
import com.walley.app.data.local.BudgetDao
import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentWithTransactions
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val investmentDao: InvestmentDao,
    private val accountOperationDao: AccountOperationDao,
    private val budgetDao: BudgetDao,
    private val adHocBudgetDao: AdHocBudgetDao
) : AccountRepository {

    // Investment accounts' displayed balance is uninvested cash (the stored balance column)
    // plus the current market value of the investments associated with them.
    override fun observeAccounts(): Flow<List<Account>> =
        combine(
            accountDao.observeAll(),
            investmentDao.observeAll(),
            investmentDao.observeAllTransactions()
        ) { accounts, investments, transactions ->
            accounts
                .map { entity ->
                    val account = entity.toDomain()
                    if (account.type == AccountType.INVESTMENT) {
                        val linked = investments.filter { it.accountId == entity.id }.map { investmentEntity ->
                            InvestmentWithTransactions(
                                investment = investmentEntity.toDomain(),
                                transactions = transactions
                                    .filter { it.investmentId == investmentEntity.id }
                                    .map { it.toDomain() }
                            )
                        }
                        account.copy(
                            balance = account.balance + investmentsValue(linked),
                            investmentCostBasis = investmentsCostBasis(linked)
                        )
                    } else {
                        account
                    }
                }
                .sortedWith(compareBy({ ACCOUNT_TYPE_ORDER[it.type] ?: Int.MAX_VALUE }, { it.name }))
        }

    private fun investmentsValue(investments: List<InvestmentWithTransactions>): BigDecimal =
        investments
            .fold(BigDecimal.ZERO) { acc, investment -> acc + investment.currentValue }
            .setScale(2, RoundingMode.HALF_UP)

    private fun investmentsCostBasis(investments: List<InvestmentWithTransactions>): BigDecimal =
        investments
            .fold(BigDecimal.ZERO) { acc, investment -> acc + investment.costBasis }
            .setScale(2, RoundingMode.HALF_UP)

    override suspend fun addAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        initialBalance: BigDecimal,
        taxRate: AccountTaxRate,
        targetAmount: BigDecimal?,
        commissionFlat: BigDecimal,
        commissionPercent: BigDecimal,
        isVirtual: Boolean
    ) {
        val isFirstAccount = accountDao.count() == 0
        accountDao.insert(
            AccountEntity(
                name = name,
                type = type,
                currency = currency,
                balanceMinorUnits = initialBalance.toMinorUnits(),
                taxRate = taxRate,
                targetAmountMinorUnits = targetAmount?.toMinorUnits(),
                isDefault = isFirstAccount,
                commissionFlatMinorUnits = commissionFlat.toMinorUnits(),
                commissionPercent = commissionPercent,
                isVirtual = isVirtual
            )
        )
    }

    override suspend fun updateAccount(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        newBalance: BigDecimal,
        targetAmount: BigDecimal?,
        commissionFlat: BigDecimal,
        commissionPercent: BigDecimal,
        isVirtual: Boolean
    ) {
        accountDao.update(
            accountId,
            name,
            type,
            taxRate,
            newBalance.toMinorUnits(),
            targetAmount?.toMinorUnits(),
            commissionFlat.toMinorUnits(),
            commissionPercent,
            isVirtual
        )
    }

    override suspend fun deleteAccount(accountId: Long) {
        if (investmentDao.countForAccount(accountId) > 0) {
            throw AccountHasLinkedInvestmentsException()
        }
        if (budgetDao.countItemsForAccountWithStatus(accountId, BudgetStatus.ACTIVE) > 0 ||
            adHocBudgetDao.countActiveForAccount(accountId) > 0 ||
            adHocBudgetDao.countActiveItemsForAccount(accountId) > 0
        ) {
            throw AccountHasLinkedActiveBudgetException()
        }
        accountOperationDao.deleteForAccount(accountId)
        accountDao.delete(accountId)
        // Maintain the invariant that an account is default whenever any accounts remain.
        if (accountDao.countDefault() == 0) {
            accountDao.firstAccountId()?.let { accountDao.setDefaultAccount(it) }
        }
    }

    override suspend fun addToBalance(accountId: Long, delta: BigDecimal) {
        accountDao.addToBalance(accountId, delta.toMinorUnits())
    }

    override suspend fun updateBalance(accountId: Long, newBalance: BigDecimal) {
        accountDao.updateBalance(accountId, newBalance.toMinorUnits())
    }

    override suspend fun setDefaultAccount(accountId: Long) {
        accountDao.setDefaultAccount(accountId)
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
