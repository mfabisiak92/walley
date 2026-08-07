package com.walley.app.feature.investments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountOperationRepository
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.IntegrationsRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.PriceFetchOutcome
import com.walley.app.data.repository.WatchedEquityRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.WatchedEquityWithNotes
import com.walley.app.domain.model.dividendsPaid
import com.walley.app.domain.model.netDividendsPaid
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InvestmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: InvestmentRepository,
    private val integrationsRepository: IntegrationsRepository,
    accountRepository: AccountRepository,
    watchedEquityRepository: WatchedEquityRepository,
    accountOperationRepository: AccountOperationRepository
) : ViewModel() {

    private val investmentId: Long = checkNotNull(savedStateHandle["investmentId"])

    val marketDataConfigured: StateFlow<Boolean> = integrationsRepository.observeYahooFinanceEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _isRefreshingPrice = MutableStateFlow(false)
    val isRefreshingPrice: StateFlow<Boolean> = _isRefreshingPrice.asStateFlow()

    private val _priceNotFoundReason = MutableStateFlow<String?>(null)
    val priceNotFoundReason: StateFlow<String?> = _priceNotFoundReason.asStateFlow()

    val investmentWithTransactions: StateFlow<InvestmentWithTransactions?> = repository.observeInvestment(investmentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The strategy currently linked to this investment, if any. */
    val strategy: StateFlow<WatchedEquityWithNotes?> = watchedEquityRepository.observeStrategiesByInvestmentId()
        .map { it[investmentId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The account this investment is linked to, and every other investment sharing that account's cash pool. */
    val linkedAccount: StateFlow<Account?> = combine(
        investmentWithTransactions,
        accountRepository.observeAccounts()
    ) { current, accounts -> accounts.find { it.id == current?.investment?.accountId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Best-effort dividends received so far for this investment — see [dividendsPaid]/[netDividendsPaid]'s docs for how they're matched. */
    val dividendsSummary: StateFlow<DividendsSummary> = combine(
        investmentWithTransactions,
        linkedAccount,
        accountOperationRepository.observeAll()
    ) { current, account, allOperations ->
        val investment = current?.investment
        if (investment == null || account == null) {
            DividendsSummary.ZERO
        } else {
            val operations = allOperations.filter { it.accountId == account.id }
            DividendsSummary(gross = investment.dividendsPaid(operations), net = investment.netDividendsPaid(operations))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DividendsSummary.ZERO)

    val investmentsInAccount: StateFlow<List<InvestmentWithTransactions>> = combine(
        investmentWithTransactions,
        repository.observeInvestments()
    ) { current, all ->
        val accountId = current?.investment?.accountId
        if (accountId == null) emptyList() else all.filter { it.investment.accountId == accountId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addTransaction(
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal
    ) {
        viewModelScope.launch { repository.addTransaction(investmentId, type, date, quantity, pricePerUnit, commission) }
    }

    fun updateTransaction(
        transactionId: Long,
        type: InvestmentTransactionType,
        date: LocalDate,
        quantity: BigDecimal,
        pricePerUnit: BigDecimal,
        commission: BigDecimal
    ) {
        viewModelScope.launch { repository.updateTransaction(transactionId, type, date, quantity, pricePerUnit, commission) }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch { repository.deleteTransaction(transactionId) }
    }

    fun updateCurrentPrice(currentPrice: BigDecimal) {
        viewModelScope.launch { repository.updateCurrentPrice(investmentId, currentPrice) }
    }

    fun revertToPreviousPrice() {
        viewModelScope.launch { repository.revertToPreviousPrice(investmentId) }
    }

    fun refreshPrice() {
        if (_isRefreshingPrice.value) return
        viewModelScope.launch {
            _isRefreshingPrice.value = true
            val outcome = repository.refreshMarketPrices(listOf(investmentId))[investmentId]
            _priceNotFoundReason.value = (outcome as? PriceFetchOutcome.NotFound)?.reason
            _isRefreshingPrice.value = false
        }
    }

    fun dismissPriceNotFound() {
        _priceNotFoundReason.value = null
    }

    fun deleteInvestment(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteInvestment(investmentId)
            onDeleted()
        }
    }
}

/** [gross] is the dividend payout itself; [net] is what was actually kept after withholding tax. */
data class DividendsSummary(val gross: BigDecimal, val net: BigDecimal) {
    companion object {
        val ZERO = DividendsSummary(BigDecimal.ZERO, BigDecimal.ZERO)
    }
}
