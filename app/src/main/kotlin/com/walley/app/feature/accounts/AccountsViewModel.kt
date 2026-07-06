package com.walley.app.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountHasLinkedInvestmentsException
import com.walley.app.data.repository.AccountRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
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
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    fun dismissDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }

    fun addAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        initialBalance: BigDecimal,
        taxRate: AccountTaxRate,
        targetAmount: BigDecimal?
    ) {
        viewModelScope.launch { repository.addAccount(name, type, currency, initialBalance, taxRate, targetAmount) }
    }

    fun updateAccount(
        accountId: Long,
        name: String,
        type: AccountType,
        taxRate: AccountTaxRate,
        newBalance: BigDecimal,
        targetAmount: BigDecimal?
    ) {
        viewModelScope.launch { repository.updateAccount(accountId, name, type, taxRate, newBalance, targetAmount) }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(accountId)
            } catch (e: AccountHasLinkedInvestmentsException) {
                _deleteBlockedMessage.value =
                    "This account still has linked investments. Delete or move them first."
            }
        }
    }
}
