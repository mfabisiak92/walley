package com.walley.app.feature.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Investment
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val repository: InvestmentRepository
) : ViewModel() {

    val investments: StateFlow<List<Investment>> = repository.observeInvestments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addInvestment(name: String, ticker: String, quantity: BigDecimal, currency: Currency, price: BigDecimal) {
        viewModelScope.launch { repository.addInvestment(name, ticker, quantity, currency, price) }
    }
}
