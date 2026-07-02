package com.walley.app.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAccount(name: String, currency: Currency, initialBalance: BigDecimal) {
        viewModelScope.launch { repository.addAccount(name, currency, initialBalance) }
    }

    fun updateAccount(accountId: Long, name: String, newBalance: BigDecimal) {
        viewModelScope.launch { repository.updateAccount(accountId, name, newBalance) }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch { repository.deleteAccount(accountId) }
    }
}
