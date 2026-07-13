package com.walley.app.feature.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.IntegrationsRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.PriceFetchOutcome
import com.walley.app.domain.model.Investment
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UpdatePricesViewModel @Inject constructor(
    private val repository: InvestmentRepository,
    private val integrationsRepository: IntegrationsRepository
) : ViewModel() {

    /** Closed positions (quantity 0) are excluded — their price never affects anything, since value and gain/loss are both zero regardless of it. */
    val investments: StateFlow<List<Investment>> = repository.observeInvestments()
        .map { list -> list.filter { it.quantity.signum() != 0 }.map { it.investment } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Whether a market refresh is actually usable right now — enabled in Settings. */
    val marketDataConfigured: StateFlow<Boolean> = integrationsRepository.observeYahooFinanceEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Investment ids with a refresh in flight right now — covers both the bulk and single-item refresh. */
    private val _refreshingIds = MutableStateFlow<Set<Long>>(emptySet())
    val refreshingIds: StateFlow<Set<Long>> = _refreshingIds.asStateFlow()

    /** Investments that failed to resolve on their most recent refresh attempt, keyed to why. */
    private val _failedRefreshReasons = MutableStateFlow<Map<Long, String>>(emptyMap())
    val failedRefreshReasons: StateFlow<Map<Long, String>> = _failedRefreshReasons.asStateFlow()

    suspend fun saveAll(updates: Map<Long, BigDecimal>) {
        updates.forEach { (investmentId, currentPrice) -> repository.updateCurrentPrice(investmentId, currentPrice) }
    }

    /** Refreshes every currently-held investment. */
    fun refreshFromMarket() {
        refresh(investments.value.map { it.id })
    }

    /** Refreshes a single investment — used by each row's own refresh icon. */
    fun refreshOne(investmentId: Long) {
        refresh(listOf(investmentId))
    }

    private fun refresh(ids: List<Long>) {
        if (ids.isEmpty() || ids.any { it in _refreshingIds.value }) return
        viewModelScope.launch {
            _refreshingIds.value = _refreshingIds.value + ids
            val outcomes = repository.refreshMarketPrices(ids)
            val failedReasons = outcomes.mapNotNull { (id, outcome) ->
                (outcome as? PriceFetchOutcome.NotFound)?.let { id to it.reason }
            }.toMap()
            _failedRefreshReasons.value = (_failedRefreshReasons.value - ids.toSet()) + failedReasons
            _refreshingIds.value = _refreshingIds.value - ids
        }
    }
}
