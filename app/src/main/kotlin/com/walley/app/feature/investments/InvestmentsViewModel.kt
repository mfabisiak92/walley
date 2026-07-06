package com.walley.app.feature.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Investment
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val repository: InvestmentRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    val investments: StateFlow<List<Investment>> = repository.observeInvestments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val investmentAccounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .map { accounts -> accounts.filter { it.type == AccountType.INVESTMENT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addInvestment(
        name: String,
        ticker: String,
        quantity: BigDecimal,
        currency: Currency,
        price: BigDecimal,
        accountId: Long?
    ) {
        viewModelScope.launch { repository.addInvestment(name, ticker, quantity, currency, price, accountId) }
    }

    fun updateInvestment(
        investmentId: Long,
        name: String,
        ticker: String,
        quantity: BigDecimal,
        price: BigDecimal,
        accountId: Long?
    ) {
        viewModelScope.launch { repository.updateInvestment(investmentId, name, ticker, quantity, price, accountId) }
    }

    fun deleteInvestment(investmentId: Long) {
        viewModelScope.launch { repository.deleteInvestment(investmentId) }
    }
}
