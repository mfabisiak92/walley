package com.walley.app.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountHasLinkedActiveBudgetException
import com.walley.app.data.repository.AccountHasLinkedInvestmentsException
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository,
    settingsRepository: SettingsRepository,
    budgetRepository: BudgetRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val baseCurrency: StateFlow<Currency> = settingsRepository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    /**
     * Amount paid so far this month toward each Saving account's linked SAVINGS budget item(s),
     * keyed by account id — summed rather than taking the first match, in case more than one item
     * ever targets the same account.
     */
    val savingsPaidThisMonth: StateFlow<Map<Long, BigDecimal>> =
        LocalDate.now().let { today -> budgetRepository.observeBudgetForMonth(today.year, today.monthValue) }
            .map { budget ->
                (budget?.items ?: emptyList())
                    .filter { it.section == BudgetSectionType.SAVINGS && it.accountId != null }
                    .groupBy { it.accountId!! }
                    .mapValues { (_, items) -> items.sumOf { it.paidAmount } }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    fun dismissDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }

    private val _closeBlockedMessage = MutableStateFlow<String?>(null)
    val closeBlockedMessage: StateFlow<String?> = _closeBlockedMessage.asStateFlow()

    fun dismissCloseBlockedMessage() {
        _closeBlockedMessage.value = null
    }

    fun addAccount(
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
    ) {
        viewModelScope.launch {
            repository.addAccount(
                name,
                type,
                currency,
                initialBalance,
                taxRate,
                targetAmount,
                targetDate,
                commissionFlat,
                commissionPercent,
                isVirtual
            )
        }
    }

    fun updateAccount(
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
    ) {
        viewModelScope.launch {
            repository.updateAccount(
                accountId,
                name,
                type,
                taxRate,
                newBalance,
                targetAmount,
                targetDate,
                commissionFlat,
                commissionPercent,
                isVirtual
            )
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(accountId)
            } catch (e: AccountHasLinkedInvestmentsException) {
                _deleteBlockedMessage.value =
                    "This account still has linked investments. Delete or move them first."
            } catch (e: AccountHasLinkedActiveBudgetException) {
                _deleteBlockedMessage.value =
                    "This account is linked to an active budget item. Remove it from the budget first, " +
                        "or mark the budget completed."
            }
        }
    }

    fun setDefaultAccount(accountId: Long) {
        viewModelScope.launch { repository.setDefaultAccount(accountId) }
    }

    fun closeAccount(accountId: Long, transferToAccountId: Long?) {
        viewModelScope.launch {
            try {
                repository.closeAccount(accountId, transferToAccountId)
            } catch (e: AccountHasLinkedInvestmentsException) {
                _closeBlockedMessage.value =
                    "This account still has linked investments. Delete or move them first."
            } catch (e: AccountHasLinkedActiveBudgetException) {
                _closeBlockedMessage.value =
                    "This account is linked to an active budget item. Remove it from the budget first, " +
                        "or mark the budget completed."
            }
        }
    }

    fun reopenAccount(accountId: Long) {
        viewModelScope.launch { repository.reopenAccount(accountId) }
    }
}
