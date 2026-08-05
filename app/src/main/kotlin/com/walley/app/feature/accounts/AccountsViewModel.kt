package com.walley.app.feature.accounts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.R
import com.walley.app.data.repository.AccountHasLinkedActiveBudgetException
import com.walley.app.data.repository.AccountHasLinkedInvestmentsException
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AccountsPreferencesRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountBalanceGroup
import com.walley.app.domain.model.AccountKindFilter
import com.walley.app.domain.model.AccountSortField
import com.walley.app.domain.model.AccountStatusFilter
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AccountsFilterState
import com.walley.app.domain.model.AccountsSortState
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.SortDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AccountRepository,
    settingsRepository: SettingsRepository,
    budgetRepository: BudgetRepository,
    private val accountsPreferencesRepository: AccountsPreferencesRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val baseCurrency: StateFlow<Currency> = settingsRepository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    /** Rates for whatever [baseCurrency] currently is — used to sort accounts by balance across currencies. */
    val exchangeRates: StateFlow<ExchangeRates?> = baseCurrency
        .flatMapLatest { base -> exchangeRateRepository.observeRates(base) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Global across all three tabs — see [AccountsSortState]. */
    val sortState: StateFlow<AccountsSortState> = accountsPreferencesRepository.observeSort()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsSortState())

    /** One persisted [AccountsFilterState] per tab — precomputed since [AccountBalanceGroup] is a fixed, small set. */
    private val filterStates: Map<AccountBalanceGroup, StateFlow<AccountsFilterState>> =
        AccountBalanceGroup.entries.associateWith { group ->
            accountsPreferencesRepository.observeFilter(group)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsFilterState())
        }

    fun filterState(group: AccountBalanceGroup): StateFlow<AccountsFilterState> = filterStates.getValue(group)

    fun setSortField(field: AccountSortField) {
        viewModelScope.launch { accountsPreferencesRepository.setSortField(field) }
    }

    fun setSortDirection(direction: SortDirection) {
        viewModelScope.launch { accountsPreferencesRepository.setSortDirection(direction) }
    }

    fun resetSort() {
        viewModelScope.launch { accountsPreferencesRepository.resetSort() }
    }

    fun setStatusFilter(group: AccountBalanceGroup, status: AccountStatusFilter) {
        viewModelScope.launch { accountsPreferencesRepository.setStatusFilter(group, status) }
    }

    fun toggleCurrencyFilter(group: AccountBalanceGroup, currency: Currency) {
        viewModelScope.launch { accountsPreferencesRepository.toggleCurrencyFilter(group, currency) }
    }

    fun toggleTypeFilter(group: AccountBalanceGroup, type: AccountType) {
        viewModelScope.launch { accountsPreferencesRepository.toggleTypeFilter(group, type) }
    }

    fun setKindFilter(group: AccountBalanceGroup, kind: AccountKindFilter) {
        viewModelScope.launch { accountsPreferencesRepository.setKindFilter(group, kind) }
    }

    fun resetFilters(group: AccountBalanceGroup) {
        viewModelScope.launch { accountsPreferencesRepository.resetFilters(group) }
    }

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

    private val _transferBlockedMessage = MutableStateFlow<String?>(null)
    val transferBlockedMessage: StateFlow<String?> = _transferBlockedMessage.asStateFlow()

    fun dismissTransferBlockedMessage() {
        _transferBlockedMessage.value = null
    }

    /**
     * The dialog only ever offers same-currency, valid account pairs and caps the amount at what's
     * available, so [IllegalArgumentException] here means the picture it was built from went stale
     * between opening the dialog and confirming (e.g. an account got closed elsewhere in the meantime)
     * — surfaced as a blocked-message rather than left to crash.
     */
    fun transferBetweenAccounts(fromAccountId: Long, toAccountId: Long, amount: BigDecimal) {
        viewModelScope.launch {
            try {
                repository.transferBetweenAccounts(fromAccountId, toAccountId, amount)
            } catch (e: IllegalArgumentException) {
                _transferBlockedMessage.value = e.message
            }
        }
    }
}
