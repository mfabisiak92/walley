package com.walley.app.feature.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AssetRepository
import com.walley.app.data.repository.BudgetIsCompletedException
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.LiabilityRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.IncomeCategory
import com.walley.app.feature.home.calculateNetWorth
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    accountRepository: AccountRepository,
    assetRepository: AssetRepository,
    liabilityRepository: LiabilityRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val budgetId: Long = checkNotNull(savedStateHandle["budgetId"])

    val budget: StateFlow<BudgetWithItems?> = budgetRepository.observeBudget(budgetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val baseCurrencyRates: StateFlow<Pair<Currency, ExchangeRates?>> = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base -> exchangeRateRepository.observeRates(base).map { rates -> base to rates } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Currency.PLN to null)

    private val baseCurrencyRatesAndNetWorthSettings = combine(
        baseCurrencyRates,
        settingsRepository.observeIncludeSavingsInNetWorth()
    ) { (base, rates), includeSavings -> Triple(base, rates, includeSavings) }

    val categoryTargets: StateFlow<Map<BudgetSectionType, BigDecimal?>> = combine(
        settingsRepository.observeCategoryTarget(BudgetSectionType.FIXED_COSTS),
        settingsRepository.observeCategoryTarget(BudgetSectionType.OTHER_COSTS),
        settingsRepository.observeCategoryTarget(BudgetSectionType.SAVINGS),
        settingsRepository.observeCategoryTarget(BudgetSectionType.INVESTMENTS)
    ) { fixed, other, savings, investments ->
        mapOf(
            BudgetSectionType.FIXED_COSTS to fixed,
            BudgetSectionType.OTHER_COSTS to other,
            BudgetSectionType.SAVINGS to savings,
            BudgetSectionType.INVESTMENTS to investments
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Current net worth plus this budget's remaining unpaid items, i.e. "if I follow this budget". */
    val projectedNetWorth: StateFlow<BigDecimal?> = combine(
        budget,
        accountRepository.observeAccounts(),
        assetRepository.observeAssets(),
        liabilityRepository.observeLiabilities(),
        baseCurrencyRatesAndNetWorthSettings
    ) { budgetWithItems, accounts, assets, liabilities, (base, rates, includeSavings) ->
        val items = budgetWithItems?.items ?: return@combine null
        val currentNetWorth = calculateNetWorth(accounts, assets, liabilities, base, rates, includeSavings)
            ?: return@combine null
        val delta = projectedNetWorthDelta(items, base, rates) ?: return@combine null
        (currentNetWorth + delta).setScale(2, RoundingMode.HALF_UP)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { budgetRepository.checkAndAutoCompleteDueItems() }
    }

    private val _deleteBlockedMessage = MutableStateFlow<String?>(null)
    val deleteBlockedMessage: StateFlow<String?> = _deleteBlockedMessage.asStateFlow()

    fun dismissDeleteBlockedMessage() {
        _deleteBlockedMessage.value = null
    }

    private val isEditable: Boolean get() = budget.value?.budget?.status != BudgetStatus.COMPLETED

    fun markPaid(itemId: Long) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.markItemPaid(itemId) }
    }

    fun markPartiallyPaid(itemId: Long, amount: BigDecimal) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.markItemPartiallyPaid(itemId, amount) }
    }

    fun updateItemAmount(itemId: Long, amount: BigDecimal) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.updateItemAmount(itemId, amount) }
    }

    fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.updateItemIcon(itemId, icon) }
    }

    fun updateItemAccount(itemId: Long, accountId: Long?) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.updateItemAccount(itemId, accountId) }
    }

    fun deleteBudget(onDeleted: () -> Unit) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(budgetId)
                onDeleted()
            } catch (e: BudgetIsCompletedException) {
                _deleteBlockedMessage.value = "This budget is marked as completed and can't be deleted."
            }
        }
    }

    fun deleteItem(itemId: Long) {
        if (!isEditable) return
        viewModelScope.launch { budgetRepository.deleteBudgetItem(itemId) }
    }

    fun restoreItem(item: BudgetItem) {
        viewModelScope.launch { budgetRepository.restoreBudgetItem(item) }
    }

    fun addItem(
        section: BudgetSectionType,
        name: String,
        amount: BigDecimal,
        currency: Currency,
        accountId: Long?,
        paymentDay: Int?,
        paymentDayIsLastOfMonth: Boolean,
        incomeCategory: IncomeCategory?,
        icon: BudgetItemIcon?
    ) {
        if (!isEditable) return
        viewModelScope.launch {
            budgetRepository.addBudgetItem(
                budgetId, section, name, amount, currency, accountId,
                paymentDay, paymentDayIsLastOfMonth, incomeCategory, icon
            )
        }
    }

    fun markCompleted() {
        viewModelScope.launch { budgetRepository.markBudgetCompleted(budgetId) }
    }
}
