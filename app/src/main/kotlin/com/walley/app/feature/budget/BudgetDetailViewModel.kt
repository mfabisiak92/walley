package com.walley.app.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.BudgetIsCompletedException
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    accountRepository: AccountRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val budgetId: Long = checkNotNull(savedStateHandle["budgetId"])

    val budget: StateFlow<BudgetWithItems?> = budgetRepository.observeBudget(budgetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val plnRates: StateFlow<ExchangeRates?> = exchangeRateRepository.observeRates(Currency.PLN)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { budgetRepository.checkAndAutoCompleteDueItems() }
    }

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    fun dismissDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }

    private val isEditable: Boolean get() = budget.value?.budget?.status != BudgetStatus.COMPLETED

    fun markPaid(itemId: Long) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.markItemPaid(itemId) }
    }

    fun markPartiallyPaid(itemId: Long, amount: BigDecimal) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.markItemPartiallyPaid(itemId, amount) }
    }

    fun deleteBudget(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(budgetId)
                onDeleted()
            } catch (e: BudgetIsCompletedException) {
                _deleteBlockedMessage.value = "This budget is marked as completed and can't be deleted."
            }
        }
    }

    fun deleteItem(itemId: Long) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.deleteBudgetItem(itemId) }
    }

    fun restoreItem(item: BudgetItem) {
        viewModelScope.launch { budgetRepository.restoreBudgetItem(item) }
    }

    fun markCompleted() {
        viewModelScope.launch { budgetRepository.markBudgetCompleted(budgetId) }
    }
}
