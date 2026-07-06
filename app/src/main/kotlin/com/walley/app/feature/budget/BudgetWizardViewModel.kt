package com.walley.app.feature.budget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

@HiltViewModel
class BudgetWizardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetRepository: BudgetRepository,
    accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val cloneFromBudgetId: Long? = savedStateHandle.get<Long>("cloneFromBudgetId")?.takeIf { it > 0 }

    var year by mutableIntStateOf(LocalDate.now().year)
        private set
    var month by mutableIntStateOf(LocalDate.now().monthValue)
        private set
    var currentStep by mutableIntStateOf(WIZARD_STEP_MONTH)
        private set
    var monthTaken by mutableIntStateOf(0)
        private set

    private val itemsBySection = mutableStateMapOf<BudgetSectionType, List<WizardItemDraft>>().apply {
        BudgetSectionType.entries.forEach { put(it, emptyList()) }
    }

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val plnRates: StateFlow<com.walley.app.domain.model.ExchangeRates?> =
        exchangeRateRepository.observeRates(Currency.PLN)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        cloneFromBudgetId?.let { sourceBudgetId ->
            viewModelScope.launch {
                val source = budgetRepository.observeBudget(sourceBudgetId).first() ?: return@launch
                val nextMonth = source.budget.yearMonth.plusMonths(1)
                year = nextMonth.year
                month = nextMonth.monthValue
                source.items.forEachIndexed { index, item ->
                    itemsBySection[item.section] = itemsFor(item.section) + WizardItemDraft(
                        localId = System.nanoTime() + index,
                        name = item.name,
                        amount = item.amount,
                        currency = item.currency,
                        accountId = item.accountId,
                        paymentDay = item.paymentDay,
                        paymentDayIsLastOfMonth = item.paymentDayIsLastOfMonth
                    )
                }
            }
        }
    }

    fun accountsFor(section: BudgetSectionType): List<Account> = when (section) {
        BudgetSectionType.SAVINGS -> accounts.value.filter { it.type == AccountType.SAVING }
        BudgetSectionType.INVESTMENTS -> accounts.value.filter { it.type == AccountType.INVESTMENT }
        else -> emptyList()
    }

    fun itemsFor(section: BudgetSectionType): List<WizardItemDraft> = itemsBySection[section].orEmpty()

    fun setYearMonth(newYear: Int, newMonth: Int) {
        year = newYear
        month = newMonth
    }

    suspend fun refreshMonthTaken() {
        monthTaken = if (budgetRepository.monthHasBudget(year, month)) 1 else -1
    }

    fun clearMonthTaken() {
        monthTaken = 0
    }

    fun goNext() {
        if (currentStep < WIZARD_STEP_SUMMARY) currentStep++
    }

    fun goBack() {
        if (currentStep > WIZARD_STEP_MONTH) currentStep--
    }

    fun sectionForStep(step: Int): BudgetSectionType? = SECTION_STEPS[step]

    fun addItem(section: BudgetSectionType, draft: WizardItemDraft) {
        itemsBySection[section] = itemsFor(section) + draft
    }

    fun removeItem(section: BudgetSectionType, localId: Long) {
        itemsBySection[section] = itemsFor(section).filterNot { it.localId == localId }
    }

    fun updateItem(section: BudgetSectionType, localId: Long, draft: WizardItemDraft) {
        itemsBySection[section] = itemsFor(section).map { if (it.localId == localId) draft else it }
    }

    /** Converts an amount to PLN using cached rates; null if a needed rate is unavailable. */
    fun convertToPln(amount: BigDecimal, currency: Currency): BigDecimal? {
        if (currency == Currency.PLN) return amount
        val rate = plnRates.value?.rates?.get(currency) ?: return null
        return amount.divide(rate, 4, RoundingMode.HALF_UP)
    }

    /** Sum of a section's items converted to PLN; null if a needed rate is unavailable. */
    fun sectionTotalPln(section: BudgetSectionType): BigDecimal? {
        var total = BigDecimal.ZERO
        for (draft in itemsFor(section)) {
            total += convertToPln(draft.amount, draft.currency) ?: return null
        }
        return total
    }

    val totalIncomePln: BigDecimal get() = sectionTotalPln(BudgetSectionType.INCOME) ?: BigDecimal.ZERO
    val totalIncomeExpensesPln: BigDecimal get() = sectionTotalPln(BudgetSectionType.INCOME_RELATED_EXPENSES) ?: BigDecimal.ZERO
    val disposableIncomePln: BigDecimal get() = totalIncomePln - totalIncomeExpensesPln

    /** Unallocated amount after Fixed/Other/Savings/Investments; null if conversion unavailable. */
    fun unallocatedPln(): BigDecimal? {
        val fixed = sectionTotalPln(BudgetSectionType.FIXED_COSTS) ?: return null
        val other = sectionTotalPln(BudgetSectionType.OTHER_COSTS) ?: return null
        val savings = sectionTotalPln(BudgetSectionType.SAVINGS) ?: return null
        val investments = sectionTotalPln(BudgetSectionType.INVESTMENTS) ?: return null
        return disposableIncomePln - fixed - other - savings - investments
    }

    suspend fun createBudget(): Long {
        val allItems = mutableListOf<BudgetItem>()
        BudgetSectionType.entries.forEach { section ->
            itemsFor(section).forEach { draft ->
                allItems += BudgetItem(
                    section = section,
                    name = draft.name,
                    amount = draft.amount,
                    currency = draft.currency,
                    accountId = draft.accountId,
                    paymentDay = draft.paymentDay,
                    paymentDayIsLastOfMonth = draft.paymentDayIsLastOfMonth
                )
            }
        }
        return budgetRepository.createBudget(year, month, allItems)
    }
}
