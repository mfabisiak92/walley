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
     * used to link something new, and excluded from net worth. If the account has a nonzero balance
     * (for [AccountType.INVESTMENT], its uninvested cash), an optional [transferToAccountId] sweeps
     * the leftover balance into that account, which must share the source's currency; passing null
     * just closes the account with its balance left untouched (frozen, since it's now excluded from
     * net worth). A virtual account may only transfer into another virtual account — its balance is
     * just an earmarked slice of a real account's money, not money of its own to move anywhere else.
     * A non-virtual account may transfer into any other open account regardless of type or virtual-ness.
     * @throws IllegalArgumentException if [transferToAccountId] is non-null but fails validation
     * (same account, mismatched currency, closed, or — for a virtual source — a non-virtual destination).
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
