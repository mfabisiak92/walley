package com.walley.app.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountBalanceGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class UpdateBalancesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AccountRepository
) : ViewModel() {

    val group: AccountBalanceGroup = AccountBalanceGroup.valueOf(checkNotNull(savedStateHandle["group"]))

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .map { accounts -> accounts.filter { it.type in group.types } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun saveAll(updates: Map<Long, BigDecimal>) {
        updates.forEach { (accountId, balance) -> repository.updateBalance(accountId, balance) }
    }
}
