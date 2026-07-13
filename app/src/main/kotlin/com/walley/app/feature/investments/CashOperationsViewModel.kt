package com.walley.app.feature.investments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountOperationRepository
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountOperation
import com.walley.app.domain.model.InvestmentWithTransactions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CashOperationsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountRepository: AccountRepository,
    accountOperationRepository: AccountOperationRepository,
    investmentRepository: InvestmentRepository
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle["accountId"])

    val account: StateFlow<Account?> = accountRepository.observeAccounts()
        .map { accounts -> accounts.find { it.id == accountId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val operations: StateFlow<List<AccountOperation>> = accountOperationRepository.observeForAccount(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val investmentsInAccount: StateFlow<List<InvestmentWithTransactions>> = investmentRepository.observeInvestments()
        .map { investments -> investments.filter { it.investment.accountId == accountId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
