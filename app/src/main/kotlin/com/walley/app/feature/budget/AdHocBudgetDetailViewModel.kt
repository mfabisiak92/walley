package com.walley.app.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AdHocBudgetRepository
import com.walley.app.data.repository.BudgetIsCompletedException
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.AdHocBudgetWithItems
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.effectiveAccountId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    private val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The budget's default account — what items draw from unless they override it. */
    val account: StateFlow<Account?> = combine(budget, accounts) { budgetWithItems, accountList ->
        budgetWithItems?.let { accountList.find { account -> account.id == it.budget.accountId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Selectable accounts a new/reassigned item can draw from — same restriction as the create wizard. */
    val savingAccounts: StateFlow<List<Account>> = accounts
        .map { accountList -> accountList.filter { it.type == AccountType.SAVING && !it.isClosed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The account a given item actually draws from: its own override, or the budget's default. */
    fun accountFor(item: AdHocBudgetItem): Account? {
        val currentBudget = budget.value?.budget ?: return null
        return accounts.value.find { it.id == item.effectiveAccountId(currentBudget) }
    }

    fun currencyFor(accountId: Long): Currency? = accounts.value.find { it.id == accountId }?.currency

    private val isEditable: Boolean get() = budget.value?.isCompleted != true

    fun markPaid(itemId: Long) {
        if (!isEditable) return
        viewModelScope.launch { repository.markItemPaid(itemId) }
    }

    fun markPartiallyPaid(itemId: Long, amount: BigDecimal) {
        if (!isEditable) return
        viewModelScope.launch { repository.markItemPartiallyPaid(itemId, amount) }
    }

    fun updateItemAmount(itemId: Long, amount: BigDecimal) {
        if (!isEditable) return
        viewModelScope.launch { repository.updateItemAmount(itemId, amount) }
    }

    fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?) {
        if (!isEditable) return
        viewModelScope.launch { repository.updateItemIcon(itemId, icon) }
    }

    fun deleteItem(itemId: Long) {
        if (!isEditable) return
        viewModelScope.launch { repository.deleteBudgetItem(itemId) }
    }

    fun restoreItem(item: AdHocBudgetItem) {
        viewModelScope.launch { repository.restoreBudgetItem(item) }
    }

    fun addItem(name: String, amount: BigDecimal, accountId: Long?, icon: BudgetItemIcon?) {
        if (!isEditable) return
        viewModelScope.launch { repository.addBudgetItem(budgetId, name, amount, accountId, icon) }
    }

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    fun dismissDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }

    fun deleteBudget(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAdHocBudget(budgetId)
                onDeleted()
            } catch (e: BudgetIsCompletedException) {
                _deleteBlockedMessage.value = "This budget is marked as completed and can't be deleted."
            }
        }
    }

    fun markCompleted() {
        viewModelScope.launch { repository.markCompleted(budgetId) }
    }
}
