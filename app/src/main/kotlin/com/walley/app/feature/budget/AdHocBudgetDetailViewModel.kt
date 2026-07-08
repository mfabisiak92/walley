package com.walley.app.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AdHocBudgetRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.AdHocBudgetWithItems
import com.walley.app.domain.model.BudgetItemIcon
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AdHocBudgetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AdHocBudgetRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    private val budgetId: Long = checkNotNull(savedStateHandle["adHocBudgetId"])

    val budget: StateFlow<AdHocBudgetWithItems?> = repository.observeAdHocBudget(budgetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The single account this budget's items are withdrawn from. */
    val account: StateFlow<Account?> = combine(budget, accountRepository.observeAccounts()) { budgetWithItems, accounts ->
        budgetWithItems?.let { accounts.find { account -> account.id == it.budget.accountId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markPaid(itemId: Long) {
        viewModelScope.launch { repository.markItemPaid(itemId) }
    }

    fun markPartiallyPaid(itemId: Long, amount: BigDecimal) {
        viewModelScope.launch { repository.markItemPartiallyPaid(itemId, amount) }
    }

    fun updateItemAmount(itemId: Long, amount: BigDecimal) {
        viewModelScope.launch { repository.updateItemAmount(itemId, amount) }
    }

    fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?) {
        viewModelScope.launch { repository.updateItemIcon(itemId, icon) }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch { repository.deleteBudgetItem(itemId) }
    }

    fun restoreItem(item: AdHocBudgetItem) {
        viewModelScope.launch { repository.restoreBudgetItem(item) }
    }

    fun deleteBudget(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAdHocBudget(budgetId)
            onDeleted()
        }
    }

    fun updateApplyAccountEffects(enabled: Boolean) {
        viewModelScope.launch { repository.updateApplyAccountEffects(budgetId, enabled) }
    }
}
