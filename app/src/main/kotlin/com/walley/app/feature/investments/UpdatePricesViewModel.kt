package com.walley.app.feature.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.IntegrationsRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.PriceFetchOutcome
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Investment
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UpdatePricesViewModel @Inject constructor(
    private val repository: InvestmentRepository,
    private val accountRepository: AccountRepository,
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
     * Generates a preview of balance changes for the given price updates and stores it.
     * Returns before/after balances for accounts and their investments.
     */
    suspend fun generateReview(priceUpdates: Map<Long, BigDecimal>): PriceUpdateReview {
        val allInvestmentsWithTransactions = repository.observeInvestments().firstOrNull() ?: emptyList()
        val accounts = accountRepository.observeAccounts().firstOrNull() ?: emptyList()

        // Only investment accounts hold priced positions; other account types are unaffected by a price update.
        val investmentAccounts = accounts.filter { it.type == AccountType.INVESTMENT }

        val accountChanges = investmentAccounts.mapNotNull { account ->
            val investmentsInAccount = allInvestmentsWithTransactions.filter { it.investment.accountId == account.id }
            if (investmentsInAccount.isEmpty()) return@mapNotNull null

            var afterAccountValue = BigDecimal.ZERO
            val investmentChanges = investmentsInAccount.mapNotNull { invWithTx ->
                val investment = invWithTx.investment
                val newPrice = priceUpdates[investment.id] ?: investment.currentPrice
                val beforeValue = invWithTx.currentValue
                val afterValue = invWithTx.quantity * newPrice
                afterAccountValue += afterValue

                if (beforeValue.compareTo(afterValue) != 0) {
                    InvestmentBalanceChange(
                        investmentId = investment.id,
                        name = investment.name,
                        ticker = investment.ticker,
                        beforeBalance = beforeValue,
                        afterBalance = afterValue
                    )
                } else {
                    null
                }
            }

            // account.balance/uninvestedCash/investmentCostBasis already reflect the current (pre-edit)
            // prices — repository.observeAccounts() folds live investment values into balance. Cost
            // basis is derived from purchase price, not current price, so it (and uninvestedCash) stays
            // put; only balance moves with the edited prices. A plain .copy(balance = ...) is therefore
            // enough to get the "after" account's own netWorthValue — which subtracts tax owed on the
            // unrealized gain — via the same formula Account already uses everywhere else in the app.
            val afterAccountBalance = account.uninvestedCash + afterAccountValue
            val afterAccount = account.copy(balance = afterAccountBalance)

            AccountBalanceChange(
                accountId = account.id,
                accountName = account.name,
                accountCurrencySymbol = account.currency.symbol,
                beforeAccountBalance = account.balance,
                afterAccountBalance = afterAccountBalance,
                beforeNetBalance = account.netWorthValue,
                afterNetBalance = afterAccount.netWorthValue,
                investments = investmentChanges
            )
        }

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
