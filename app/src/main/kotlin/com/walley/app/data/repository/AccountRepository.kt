package com.walley.app.data.repository

import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun addAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        initialBalance: BigDecimal,
        taxRate: AccountTaxRate,
        targetAmount: BigDecimal?,
        targetDate: LocalDate?,
        commissionFlat: BigDecimal = BigDecimal.ZERO,
        commissionPercent: BigDecimal = BigDecimal.ZERO,
        isVirtual: Boolean = false
    )
    suspend fun updateAccount(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        newBalance: BigDecimal,
        targetAmount: BigDecimal?,
        targetDate: LocalDate?,
        commissionFlat: BigDecimal = BigDecimal.ZERO,
        commissionPercent: BigDecimal = BigDecimal.ZERO,
        isVirtual: Boolean = false
    )
    /**
     * @throws AccountHasLinkedInvestmentsException if the account still has linked investments.
     * @throws AccountHasLinkedActiveBudgetException if the account is still referenced by an active
     * monthly budget item or an incomplete ad-hoc budget.
     */
    suspend fun deleteAccount(accountId: Long)
    /**
     * Soft-closes an account: kept in the DB (reversible via [reopenAccount]), hidden from pickers
     * used to link something new, and excluded from net worth. Non-virtual accounts with a nonzero
     * balance (for [AccountType.INVESTMENT], its uninvested cash) require [transferToAccountId] —
     * always [AccountType.CHECKING], [AccountType.SAVING], or [AccountType.CASH] — to sweep the
     * leftover balance into; virtual accounts and zero-balance accounts close directly, ignoring
     * [transferToAccountId].
     * @throws IllegalArgumentException if [transferToAccountId] fails validation (same account,
     * mismatched currency, closed, or an unsupported type).
     * @throws AccountHasLinkedInvestmentsException if an Investment account still has linked investments.
     * @throws AccountHasLinkedActiveBudgetException if the account is still referenced by an active
     * monthly budget item or an incomplete ad-hoc budget.
     */
    suspend fun closeAccount(accountId: Long, transferToAccountId: Long?)
    /** Reverses [closeAccount], making the account selectable again. Doesn't move any balance back. */
    suspend fun reopenAccount(accountId: Long)
    /** Adds (or subtracts, for a negative delta) an amount to an account's stored balance. */
    suspend fun addToBalance(accountId: Long, delta: BigDecimal)
    /**
     * Overwrites an account's raw stored balance directly, leaving every other field untouched.
     * For an Investment account this sets its **uninvested cash**, same as [updateAccount]'s newBalance.
     */
    suspend fun updateBalance(accountId: Long, newBalance: BigDecimal)
    /** Makes this the one default account; every other account becomes non-default. */
    suspend fun setDefaultAccount(accountId: Long)
}

class AccountHasLinkedInvestmentsException : Exception("Account has linked investments")
class AccountHasLinkedActiveBudgetException : Exception("Account is linked to an active budget")
