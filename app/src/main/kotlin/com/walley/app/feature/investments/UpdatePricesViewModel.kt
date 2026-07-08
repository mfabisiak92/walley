package com.walley.app.feature.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.domain.model.Investment
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class UpdatePricesViewModel @Inject constructor(
    private val repository: InvestmentRepository
) : ViewModel() {

    val investments: StateFlow<List<Investment>> = repository.observeInvestments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun saveAll(updates: Map<Long, BigDecimal>) {
        updates.forEach { (investmentId, currentPrice) -> repository.updateCurrentPrice(investmentId, currentPrice) }
    }
}
