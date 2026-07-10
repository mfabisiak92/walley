package com.walley.app.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AdHocBudgetRepository
import com.walley.app.domain.model.AdHocBudgetWithItems
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AdHocBudgetsViewModel @Inject constructor(
    repository: AdHocBudgetRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val budgets: StateFlow<List<AdHocBudgetWithItems>> = repository.observeAdHocBudgetsWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val accounts = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun currencyFor(accountId: Long): Currency? = accounts.value.find { it.id == accountId }?.currency
}
