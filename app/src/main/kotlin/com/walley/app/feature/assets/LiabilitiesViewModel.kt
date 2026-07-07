package com.walley.app.feature.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.LiabilityRepository
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.Liability
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LiabilitiesViewModel @Inject constructor(
    private val repository: LiabilityRepository
) : ViewModel() {

    val liabilities: StateFlow<List<Liability>> = repository.observeLiabilities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addLiability(
        name: String,
        currency: Currency,
        originalAmount: BigDecimal,
        currentBalance: BigDecimal,
        startDate: LocalDate
    ) {
        viewModelScope.launch { repository.addLiability(name, currency, originalAmount, currentBalance, startDate) }
    }

    fun updateCurrentBalance(liabilityId: Long, currentBalance: BigDecimal) {
        viewModelScope.launch { repository.updateCurrentBalance(liabilityId, currentBalance) }
    }

    fun deleteLiability(liabilityId: Long) {
        viewModelScope.launch { repository.deleteLiability(liabilityId) }
    }
}
