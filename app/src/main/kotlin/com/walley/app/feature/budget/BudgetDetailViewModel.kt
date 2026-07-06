package com.walley.app.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.BudgetWithItems
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    private val budgetId: Long = checkNotNull(savedStateHandle["budgetId"])

    val budget: StateFlow<BudgetWithItems?> = budgetRepository.observeBudget(budgetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { budgetRepository.checkAndAutoCompleteDueItems() }
    }

    fun markPaid(itemId: Long) {
        viewModelScope.launch { budgetRepository.markItemPaid(itemId) }
    }

    fun markPartiallyPaid(itemId: Long, amount: BigDecimal) {
        viewModelScope.launch { budgetRepository.markItemPartiallyPaid(itemId, amount) }
    }

    fun deleteBudget(onDeleted: () -> Unit) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budgetId)
            onDeleted()
        }
    }
}
