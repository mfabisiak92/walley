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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class UpdatePricesViewModel @Inject constructor(
    private val repository: InvestmentRepository
) : ViewModel() {

    /** Closed positions (quantity 0) are excluded — their price never affects anything, since value and gain/loss are both zero regardless of it. */
    val investments: StateFlow<List<Investment>> = repository.observeInvestments()
        .map { list -> list.filter { it.quantity.signum() != 0 }.map { it.investment } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun saveAll(updates: Map<Long, BigDecimal>) {
        updates.forEach { (investmentId, currentPrice) -> repository.updateCurrentPrice(investmentId, currentPrice) }
    }
}
