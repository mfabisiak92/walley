package com.walley.app.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.BudgetIsCompletedException
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetRowData(
    val budgetWithItems: BudgetWithItems,
    val disposableIncome: BigDecimal?,
    val unallocated: BigDecimal?,
    val baseCurrency: Currency,
    val progress: BudgetProgress?
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: BudgetRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val baseCurrencyRates = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base -> exchangeRateRepository.observeRates(base).map { rates -> base to rates } }

    val budgets: StateFlow<List<BudgetRowData>> = combine(
        repository.observeBudgetsWithItems(),
        baseCurrencyRates
    ) { budgetsWithItems, (baseCurrency, baseRates) ->
        budgetsWithItems.map { budgetWithItems -> toRowData(budgetWithItems, baseCurrency, baseRates) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    init {
        viewModelScope.launch { repository.checkAndAutoCompleteDueItems() }
    }

    private fun toRowData(
        budgetWithItems: BudgetWithItems,
        baseCurrency: Currency,
        baseRates: ExchangeRates?
    ): BudgetRowData = BudgetRowData(
        budgetWithItems = budgetWithItems,
        disposableIncome = disposableIncome(budgetWithItems.items, baseCurrency, baseRates),
        unallocated = unallocatedAmount(budgetWithItems.items, baseCurrency, baseRates),
        baseCurrency = baseCurrency,
        progress = budgetProgress(budgetWithItems.items, SPENDING_SECTIONS, baseCurrency, baseRates)
    )

    fun dismissDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteBudget(budgetId)
            } catch (e: BudgetIsCompletedException) {
                _deleteBlockedMessage.value = "This budget is marked as completed and can't be deleted."
            }
        }
    }
}
