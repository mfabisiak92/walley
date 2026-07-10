package com.walley.app.feature.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentWithTransactions
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
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

    val investments: StateFlow<List<InvestmentWithTransactions>> = repository.observeInvestments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val investmentAccounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .map { accounts -> accounts.filter { it.type == AccountType.INVESTMENT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addInvestment(
        name: String,
        ticker: String,
        category: InvestmentCategory,
        purchaseDate: LocalDate,
        quantity: BigDecimal,
        currency: Currency,
        price: BigDecimal,
        currentPrice: BigDecimal,
        accountId: Long,
        commission: BigDecimal = BigDecimal.ZERO
    ) {
        viewModelScope.launch {
            repository.addInvestment(
                name,
                ticker,
                category,
                currency,
                currentPrice,
                accountId,
                purchaseDate,
                quantity,
                price,
                commission
            )
        }
    }

    fun updateInvestmentDetails(
        investmentId: Long,
        name: String,
        ticker: String,
        category: InvestmentCategory,
        accountId: Long
    ) {
        viewModelScope.launch { repository.updateInvestmentDetails(investmentId, name, ticker, category, accountId) }
    }
}
