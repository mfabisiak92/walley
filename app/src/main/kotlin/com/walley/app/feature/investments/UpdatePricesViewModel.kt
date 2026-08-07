package com.walley.app.feature.investments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.IntegrationsRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.PriceFetchOutcome
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Investment
import com.walley.app.domain.model.InvestmentWithTransactions
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

/** Sentinel nav-arg default meaning "no account" — Navigation Compose has no nullable Long arg type. */
const val NO_ACCOUNT_ID = -1L

@HiltViewModel
class UpdatePricesViewModel @Inject constructor(
    private val repository: InvestmentRepository,
    private val accountRepository: AccountRepository,
    private val integrationsRepository: IntegrationsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Non-null when reached from a single account's Investments tab — scopes [investments] and [generateReview] to it instead of the whole portfolio. */
    private val accountId: Long? = savedStateHandle.get<Long>("accountId")?.takeIf { it != NO_ACCOUNT_ID }

    /** Closed positions (quantity 0) are excluded — their price never affects anything, since value and gain/loss are both zero regardless of it. */
    val investments: StateFlow<List<Investment>> = repository.observeInvestments()
        .map { list ->
            list.filter { it.quantity.signum() != 0 && (accountId == null || it.investment.accountId == accountId) }
                .map { it.investment }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Investment accounts this screen covers — just [accountId] when scoped to one, otherwise every investment account. Drives the per-account grouping and its live balance/net preview header. */
    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .map { list -> list.filter { it.type == AccountType.INVESTMENT && (accountId == null || it.id == accountId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Full transaction history per account (not just current price) — [computeAccountBalanceChanges] needs quantity/cost basis, not just the current-price snapshot [investments] exposes. */
    val investmentsByAccount: StateFlow<Map<Long, List<InvestmentWithTransactions>>> = repository.observeInvestments()
        .map { list -> list.filter { it.investment.accountId != null }.groupBy { it.investment.accountId!! } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Whether a market refresh is actually usable right now — enabled in Settings. */
    val marketDataConfigured: StateFlow<Boolean> = integrationsRepository.observeYahooFinanceEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Investment ids with a refresh in flight right now — covers both the bulk and single-item refresh. */
    private val _refreshingIds = MutableStateFlow<Set<Long>>(emptySet())
    val refreshingIds: StateFlow<Set<Long>> = _refreshingIds.asStateFlow()

    /** Investments that failed to resolve on their most recent refresh attempt, keyed to why. */
    private val _failedRefreshReasons = MutableStateFlow<Map<Long, String>>(emptyMap())
    val failedRefreshReasons: StateFlow<Map<Long, String>> = _failedRefreshReasons.asStateFlow()

    /**
     * Prices fetched from the market on this screen but not yet saved — a "Refresh" here is only a
     * preview, same as a manual edit. Nothing is written to the database until the user reviews and
     * taps "Confirm & save"; only [saveReview] (or the plain [saveAll] escape hatch) ever persists.
     */
    private val _fetchedPrices = MutableStateFlow<Map<Long, BigDecimal>>(emptyMap())
    val fetchedPrices: StateFlow<Map<Long, BigDecimal>> = _fetchedPrices.asStateFlow()

    /** Current price update review being displayed. */
    private val _currentReview = MutableStateFlow<PriceUpdateReview?>(null)
    val currentReview: StateFlow<PriceUpdateReview?> = _currentReview.asStateFlow()

    suspend fun saveAll(updates: Map<Long, BigDecimal>) {
        updates.forEach { (investmentId, currentPrice) -> repository.updateCurrentPrice(investmentId, currentPrice) }
    }

    /**
     * Generates a preview of balance changes for the given price updates and stores it. Reads
     * [accounts] and [investmentsByAccount]'s current values rather than re-fetching, so the review
     * matches exactly what this screen was showing when the user tapped "Review" — this screen can't
     * reach the button before those flows have already emitted at least once.
     */
    suspend fun generateReview(priceUpdates: Map<Long, BigDecimal>): PriceUpdateReview {
        val accountChanges = computeAccountBalanceChanges(investmentsByAccount.value, accounts.value, priceUpdates)
        val review = PriceUpdateReview(
            accountChanges = accountChanges,
            priceChanges = priceUpdates
        )
        _currentReview.value = review
        return review
    }

    /**
     * Saves all prices from the current review and clears it.
     */
    suspend fun saveReview() {
        val review = _currentReview.value ?: return
        saveAll(review.priceChanges)
        _currentReview.value = null
    }

    /** Refreshes every currently-held investment. */
    fun refreshFromMarket() {
        refresh(investments.value.map { it.id })
    }

    /** Refreshes every currently-held investment linked to one account — used by that account's section header. */
    fun refreshAccount(accountId: Long) {
        refresh(investments.value.filter { it.accountId == accountId }.map { it.id })
    }

    /** Refreshes a single investment — used by each row's own refresh icon. */
    fun refreshOne(investmentId: Long) {
        refresh(listOf(investmentId))
    }

    private fun refresh(ids: List<Long>) {
        if (ids.isEmpty() || ids.any { it in _refreshingIds.value }) return
        viewModelScope.launch {
            _refreshingIds.value = _refreshingIds.value + ids
            // fetchMarketPrices (not refreshMarketPrices) — a refresh on this screen must not touch
            // the database until the user reviews and confirms it.
            val outcomes = repository.fetchMarketPrices(ids)
            val fetchedPrices = outcomes.mapNotNull { (id, outcome) ->
                (outcome as? PriceFetchOutcome.Success)?.let { id to it.price }
            }.toMap()
            val failedReasons = outcomes.mapNotNull { (id, outcome) ->
                (outcome as? PriceFetchOutcome.NotFound)?.let { id to it.reason }
            }.toMap()
            _fetchedPrices.value = (_fetchedPrices.value - ids.toSet()) + fetchedPrices
            _failedRefreshReasons.value = (_failedRefreshReasons.value - ids.toSet()) + failedReasons
            _refreshingIds.value = _refreshingIds.value - ids
        }
    }
}
