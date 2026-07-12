package com.walley.app.feature.budget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.allowedAccountTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val WIZARD_STEP_MONTH = 0
const val WIZARD_STEP_SUMMARY = 7

private val SECTION_STEPS = mapOf(
    1 to BudgetSectionType.INCOME,
    2 to BudgetSectionType.INCOME_RELATED_EXPENSES,
    3 to BudgetSectionType.FIXED_COSTS,
    4 to BudgetSectionType.OTHER_COSTS,
    5 to BudgetSectionType.SAVINGS,
    6 to BudgetSectionType.INVESTMENTS
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val cloneFromBudgetId: Long? = savedStateHandle.get<Long>("cloneFromBudgetId")?.takeIf { it > 0 }
    private val resumeBudgetId: Long? = savedStateHandle.get<Long>("resumeBudgetId")?.takeIf { it > 0 }

    /** The budget row this session is persisting to as a Draft; null until the first autosave. */
    private var draftBudgetId: Long? = resumeBudgetId

    var year by mutableIntStateOf(LocalDate.now().year)
        private set
    var month by mutableIntStateOf(LocalDate.now().monthValue)
        private set
    var currentStep by mutableIntStateOf(WIZARD_STEP_MONTH)
        private set
    var monthTaken by mutableIntStateOf(0)
        private set
    var applyIncomeAccountEffects by mutableStateOf(true)
    var applyCostsAccountEffects by mutableStateOf(true)
    var applySavingsAccountEffects by mutableStateOf(true)
    var applyInvestmentsAccountEffects by mutableStateOf(true)

    private val itemsBySection = mutableStateMapOf<BudgetSectionType, List<WizardItemDraft>>().apply {
        BudgetSectionType.entries.forEach { put(it, emptyList()) }
    }

    // Eagerly collected (rather than WhileSubscribed) so the data is already loaded by the time the
    // user reaches the first section step — that step is the first-ever subscriber, and reading
    // .value before its async query resolves would otherwise show a false "no accounts yet" state.
    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val baseCurrency: StateFlow<Currency> = settingsRepository.observeBaseCurrency()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Currency.PLN)

    private val rates: StateFlow<ExchangeRates?> = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base -> exchangeRateRepository.observeRates(base) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    init {
        cloneFromBudgetId?.let { sourceBudgetId ->
            viewModelScope.launch {
                val source = budgetRepository.observeBudget(sourceBudgetId).first() ?: return@launch
                val nextMonth = source.budget.yearMonth.plusMonths(1)
                year = nextMonth.year
                month = nextMonth.monthValue
                applyIncomeAccountEffects = source.budget.applyIncomeAccountEffects
                applyCostsAccountEffects = source.budget.applyCostsAccountEffects
                applySavingsAccountEffects = source.budget.applySavingsAccountEffects
                applyInvestmentsAccountEffects = source.budget.applyInvestmentsAccountEffects
                loadDraftItems(source.items)
            }
        }
        resumeBudgetId?.let { budgetId ->
            viewModelScope.launch {
                val source = budgetRepository.observeBudget(budgetId).first() ?: return@launch
                year = source.budget.year
                month = source.budget.month
                applyIncomeAccountEffects = source.budget.applyIncomeAccountEffects
                applyCostsAccountEffects = source.budget.applyCostsAccountEffects
                applySavingsAccountEffects = source.budget.applySavingsAccountEffects
                applyInvestmentsAccountEffects = source.budget.applyInvestmentsAccountEffects
                loadDraftItems(source.items)
                // Month is already settled for a resumed draft — land straight on the first section.
                currentStep = 1
            }
        }
    }

    private fun loadDraftItems(items: List<BudgetItem>) {
        items.forEachIndexed { index, item ->
            itemsBySection[item.section] = itemsFor(item.section) + WizardItemDraft(
                localId = System.nanoTime() + index,
                name = item.name,
                amount = item.amount,
                currency = item.currency,
                accountId = item.accountId,
                paymentDay = item.paymentDay,
                paymentDayIsLastOfMonth = item.paymentDayIsLastOfMonth,
                incomeCategory = item.incomeCategory,
                icon = item.icon
            )
        }
    }

    fun accountsFor(section: BudgetSectionType): List<Account> {
        val types = section.allowedAccountTypes() ?: return emptyList()
        return accounts.value.filter { it.type in types && !it.isClosed }
    }

    fun itemsFor(section: BudgetSectionType): List<WizardItemDraft> = itemsBySection[section].orEmpty()

    fun setYearMonth(newYear: Int, newMonth: Int) {
        year = newYear
        month = newMonth
    }

    suspend fun refreshMonthTaken() {
        monthTaken = if (budgetRepository.monthHasBudget(year, month, excludeBudgetId = draftBudgetId)) 1 else -1
    }

    fun clearMonthTaken() {
        monthTaken = 0
    }

    fun goNext() {
        if (currentStep < WIZARD_STEP_SUMMARY) {
            currentStep++
            autosaveDraft()
        }
    }

    fun goBack() {
        if (currentStep > WIZARD_STEP_MONTH) {
            // Save while still on the step being left, so its edits aren't lost once we step back.
            autosaveDraft()
            currentStep--
        }
    }

    fun sectionForStep(step: Int): BudgetSectionType? = SECTION_STEPS[step]

    fun addItem(section: BudgetSectionType, draft: WizardItemDraft) {
        itemsBySection[section] = itemsFor(section) + draft
        autosaveDraft()
    }

    fun removeItem(section: BudgetSectionType, localId: Long) {
        itemsBySection[section] = itemsFor(section).filterNot { it.localId == localId }
        autosaveDraft()
    }

    fun updateItem(section: BudgetSectionType, localId: Long, draft: WizardItemDraft) {
        itemsBySection[section] = itemsFor(section).map { if (it.localId == localId) draft else it }
        autosaveDraft()
    }

    /** Converts an amount to the Settings base currency using cached rates; null if a needed rate is unavailable. */
    fun convertToBaseCurrency(amount: BigDecimal, currency: Currency): BigDecimal? {
        val base = baseCurrency.value
        if (currency == base) return amount
        val rate = rates.value?.rates?.get(currency) ?: return null
        return amount.divide(rate, 4, RoundingMode.HALF_UP)
    }

    /** Sum of a section's items converted to the base currency; null if a needed rate is unavailable. */
    fun sectionTotal(section: BudgetSectionType): BigDecimal? {
        var total = BigDecimal.ZERO
        for (draft in itemsFor(section)) {
            total += convertToBaseCurrency(draft.amount, draft.currency) ?: return null
        }
        return total
    }

    val totalIncome: BigDecimal get() = sectionTotal(BudgetSectionType.INCOME) ?: BigDecimal.ZERO
    val totalIncomeExpenses: BigDecimal get() = sectionTotal(BudgetSectionType.INCOME_RELATED_EXPENSES) ?: BigDecimal.ZERO
    val disposableIncome: BigDecimal get() = totalIncome - totalIncomeExpenses

    /** Unallocated amount after Fixed/Other/Savings/Investments; null if conversion unavailable. */
    fun unallocatedAmount(): BigDecimal? {
        val fixed = sectionTotal(BudgetSectionType.FIXED_COSTS) ?: return null
        val other = sectionTotal(BudgetSectionType.OTHER_COSTS) ?: return null
        val savings = sectionTotal(BudgetSectionType.SAVINGS) ?: return null
        val investments = sectionTotal(BudgetSectionType.INVESTMENTS) ?: return null
        return disposableIncome - fixed - other - savings - investments
    }

    /** Fire-and-forget persistence of current progress as a Draft; safe to call frequently. */
    private fun autosaveDraft() {
        if (currentStep <= WIZARD_STEP_MONTH) return
        viewModelScope.launch {
            draftBudgetId = budgetRepository.saveDraft(
                draftBudgetId, year, month, collectItems(),
                applyIncomeAccountEffects, applyCostsAccountEffects, applySavingsAccountEffects, applyInvestmentsAccountEffects
            )
        }
    }

    private fun collectItems(): List<BudgetItem> {
        val allItems = mutableListOf<BudgetItem>()
        BudgetSectionType.entries.forEach { section ->
            itemsFor(section).forEach { draft ->
                val icon = draft.icon ?: when (section) {
                    BudgetSectionType.SAVINGS -> BudgetItemIcon.SAVING
                    BudgetSectionType.INVESTMENTS -> BudgetItemIcon.INVESTMENT
                    else -> null
                }
                allItems += BudgetItem(
                    section = section,
                    name = draft.name,
                    amount = draft.amount,
                    currency = draft.currency,
                    accountId = draft.accountId,
                    paymentDay = draft.paymentDay,
                    paymentDayIsLastOfMonth = draft.paymentDayIsLastOfMonth,
                    incomeCategory = draft.incomeCategory,
                    icon = icon
                )
            }
        }
        return allItems
    }

    /** Finalizes the wizard's budget as Active — a one-way transition out of Draft. */
    suspend fun createBudget(): Long {
        val id = budgetRepository.submitBudget(
            draftBudgetId, year, month, collectItems(),
            applyIncomeAccountEffects, applyCostsAccountEffects, applySavingsAccountEffects, applyInvestmentsAccountEffects
        )
        draftBudgetId = id
        return id
    }
}
