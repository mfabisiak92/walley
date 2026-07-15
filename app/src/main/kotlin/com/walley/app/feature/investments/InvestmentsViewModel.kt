package com.walley.app.feature.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.InvestmentsPreferencesRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.data.repository.WatchedEquityRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.InvestmentCategory
import com.walley.app.domain.model.InvestmentSortField
import com.walley.app.domain.model.InvestmentWithTransactions
import com.walley.app.domain.model.InvestmentsFilterState
import com.walley.app.domain.model.InvestmentsSortState
import com.walley.app.domain.model.PositionStatusFilter
import com.walley.app.domain.model.SortDirection
import com.walley.app.domain.model.WatchedEquityWithNotes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InvestmentsViewModel @Inject constructor(
    private val repository: InvestmentRepository,
    accountRepository: AccountRepository,
    watchedEquityRepository: WatchedEquityRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository,
    private val investmentsPreferencesRepository: InvestmentsPreferencesRepository
) : ViewModel() {

    val investments: StateFlow<List<InvestmentWithTransactions>> = repository.observeInvestments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val investmentAccounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .map { accounts -> accounts.filter { it.type == AccountType.INVESTMENT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Each investment's linked strategy, keyed by investment id. */
    val strategiesByInvestmentId: StateFlow<Map<Long, WatchedEquityWithNotes>> =
        watchedEquityRepository.observeStrategiesByInvestmentId()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val baseCurrency: StateFlow<Currency> = settingsRepository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN)

    /** Rates for whatever [baseCurrency] currently is — used to sort positions by value across currencies. */
    val exchangeRates: StateFlow<ExchangeRates?> = baseCurrency
        .flatMapLatest { base -> exchangeRateRepository.observeRates(base) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sortState: StateFlow<InvestmentsSortState> = investmentsPreferencesRepository.observeSort()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentsSortState())

    val filterState: StateFlow<InvestmentsFilterState> = investmentsPreferencesRepository.observeFilter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InvestmentsFilterState())

    fun setSortField(field: InvestmentSortField) {
        viewModelScope.launch { investmentsPreferencesRepository.setSortField(field) }
    }

    fun setSortDirection(direction: SortDirection) {
        viewModelScope.launch { investmentsPreferencesRepository.setSortDirection(direction) }
    }

    fun resetSort() {
        viewModelScope.launch { investmentsPreferencesRepository.resetSort() }
    }

    fun setStatusFilter(status: PositionStatusFilter) {
        viewModelScope.launch { investmentsPreferencesRepository.setStatusFilter(status) }
    }

    fun toggleCategoryFilter(category: InvestmentCategory) {
        viewModelScope.launch { investmentsPreferencesRepository.toggleCategoryFilter(category) }
    }

    fun toggleCurrencyFilter(currency: Currency) {
        viewModelScope.launch { investmentsPreferencesRepository.toggleCurrencyFilter(currency) }
    }

    fun toggleAccountFilter(accountId: Long) {
        viewModelScope.launch { investmentsPreferencesRepository.toggleAccountFilter(accountId) }
    }

    fun resetFilters() {
        viewModelScope.launch { investmentsPreferencesRepository.resetFilters() }
    }

    fun addInvestment(
        name: String,
        ticker: String,
        externalTicker: String?,
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
                commission,
                externalTicker
            )
        }
    }

    fun updateInvestmentDetails(
        investmentId: Long,
        name: String,
        ticker: String,
        externalTicker: String?,
        category: InvestmentCategory,
        accountId: Long
    ) {
        viewModelScope.launch {
            repository.updateInvestmentDetails(investmentId, name, ticker, category, accountId, externalTicker)
        }
    }
}
