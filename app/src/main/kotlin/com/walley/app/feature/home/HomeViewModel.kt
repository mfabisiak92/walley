package com.walley.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AssetRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.LiabilityRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.Asset
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.CurrencyTotal
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.Liability
import com.walley.app.feature.budget.BudgetProgress
import com.walley.app.feature.budget.SPENDING_SECTIONS
import com.walley.app.feature.budget.budgetProgress
import com.walley.app.feature.budget.projectedNetWorthDelta
import com.walley.app.feature.budget.unallocatedAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeBalances(
    val total: List<CurrencyTotal> = emptyList(),
    val savings: List<CurrencyTotal> = emptyList()
)

data class NetWorthByCurrency(
    val currency: Currency,
    val amountInBaseCurrency: BigDecimal,
    val percent: BigDecimal
)

/** One contributor to net worth (an account or an asset), shown in the breakdown screen. */
data class NetWorthElement(
    val name: String,
    val currency: Currency,
    val originalAmount: BigDecimal,
    val amountInBaseCurrency: BigDecimal
)

data class NetWorthState(
    val currency: Currency,
    // null when conversion is impossible (rates unavailable)
    val amount: BigDecimal?,
    // rate date shown when a conversion actually happened
    val rateDate: String?,
    // breakdown of net worth by the original currency of each account, converted to base currency
    val breakdown: List<NetWorthByCurrency> = emptyList(),
    // every account and asset that contributes to net worth, for the detail/breakdown screen
    val elements: List<NetWorthElement> = emptyList(),
    // projected net worth at the end of the current calendar month if this month's budget is followed
    // through to completion; null when there's no budget for the current month, or amount is null
    val projectedAmount: BigDecimal? = null
)

/** Snapshot of the current calendar month's budget, for a compact at-a-glance card on Home. */
data class MonthBudgetSummary(
    val progress: BudgetProgress?,
    val unallocated: BigDecimal?,
    val currency: Currency,
    val daysLeftInMonth: Int
)

/** An unpaid item with a payment day, for the "Due soon" list on Home. */
data class UpcomingBudgetItem(
    val name: String,
    val amount: BigDecimal,
    val currency: Currency,
    val icon: BudgetItemIcon?,
    // negative = overdue by that many days, 0 = due today, positive = due in that many days
    val daysUntilDue: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
    assetRepository: AssetRepository,
    liabilityRepository: LiabilityRepository,
    budgetRepository: BudgetRepository,
    settingsRepository: SettingsRepository,
    exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    val homeBalances: StateFlow<HomeBalances> = accountRepository.observeAccounts()
        .map { accounts ->
            HomeBalances(
                total = currencyTotals(accounts),
                savings = currencyTotals(accounts.filter { it.type == AccountType.SAVING })
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeBalances())

    private val baseCurrencyRates = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base ->
            exchangeRateRepository.observeRates(base).map { rates -> base to rates }
        }

    private val currentMonthBudget = LocalDate.now().let { today ->
        budgetRepository.observeBudgetForMonth(today.year, today.monthValue)
    }

    private val currentMonthBudgetItems = currentMonthBudget.map { it?.items ?: emptyList() }

    val netWorth: StateFlow<NetWorthState?> = combine(
        accountRepository.observeAccounts(),
        assetRepository.observeAssets(),
        liabilityRepository.observeLiabilities(),
        baseCurrencyRates,
        currentMonthBudgetItems
    ) { accounts, assets, liabilities, (base, rates), budgetItems ->
        if (accounts.isEmpty() && assets.isEmpty() && liabilities.isEmpty()) {
            null
        } else {
            computeNetWorth(accounts, assets, liabilities, budgetItems, base, rates)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val monthBudgetSummary: StateFlow<MonthBudgetSummary?> = combine(
        currentMonthBudgetItems,
        baseCurrencyRates
    ) { items, (base, rates) ->
        if (items.isEmpty()) {
            null
        } else {
            MonthBudgetSummary(
                progress = budgetProgress(items, SPENDING_SECTIONS, base, rates),
                unallocated = unallocatedAmount(items, base, rates),
                currency = base,
                daysLeftInMonth = (YearMonth.now().lengthOfMonth() - LocalDate.now().dayOfMonth).coerceAtLeast(0)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val upcomingItems: StateFlow<List<UpcomingBudgetItem>> = currentMonthBudget
        .map { budget -> upcomingBudgetItems(budget) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun upcomingBudgetItems(budgetWithItems: BudgetWithItems?): List<UpcomingBudgetItem> {
        val budget = budgetWithItems ?: return emptyList()
        val today = LocalDate.now()
        val yearMonth = YearMonth.of(budget.budget.year, budget.budget.month)
        return budget.items
            .filter { !it.isCompleted && it.hasPaymentDay }
            .map { item ->
                val targetDay = if (item.paymentDayIsLastOfMonth) {
                    yearMonth.lengthOfMonth()
                } else {
                    item.paymentDay!!.coerceAtMost(yearMonth.lengthOfMonth())
                }
                val targetDate = yearMonth.atDay(targetDay)
                UpcomingBudgetItem(
                    name = item.name,
                    amount = item.amount,
                    currency = item.currency,
                    icon = item.icon,
                    daysUntilDue = ChronoUnit.DAYS.between(today, targetDate).toInt()
                )
            }
            .sortedBy { it.daysUntilDue }
    }

    private fun computeNetWorth(
        accounts: List<Account>,
        assets: List<Asset>,
        liabilities: List<Liability>,
        currentMonthBudgetItems: List<BudgetItem>,
        base: Currency,
        rates: ExchangeRates?
    ): NetWorthState {
        val byCurrency = linkedMapOf<Currency, BigDecimal>()
        val elements = mutableListOf<NetWorthElement>()
        var usedRates = false

        fun convertToBase(amount: BigDecimal, currency: Currency): BigDecimal? {
            if (currency == base) return amount
            val rate = rates?.rates?.get(currency) ?: return null
            // rate is base -> currency, so convert back by dividing
            usedRates = true
            return amount.divide(rate, 10, RoundingMode.HALF_UP)
        }

        for (account in accounts) {
            // Net worth reflects investment gains after tax, not their pre-tax market value.
            val amountInBase = convertToBase(account.netWorthValue, account.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[account.currency] = (byCurrency[account.currency] ?: BigDecimal.ZERO) + amountInBase
            elements += NetWorthElement(
                name = account.name,
                currency = account.currency,
                originalAmount = account.netWorthValue,
                amountInBaseCurrency = amountInBase.setScale(2, RoundingMode.HALF_UP)
            )
        }
        for (asset in assets) {
            val amountInBase = convertToBase(asset.currentValue, asset.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[asset.currency] = (byCurrency[asset.currency] ?: BigDecimal.ZERO) + amountInBase
            elements += NetWorthElement(
                name = asset.name,
                currency = asset.currency,
                originalAmount = asset.currentValue,
                amountInBaseCurrency = amountInBase.setScale(2, RoundingMode.HALF_UP)
            )
        }
        for (liability in liabilities) {
            val amountInBase = convertToBase(liability.currentBalance, liability.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[liability.currency] = (byCurrency[liability.currency] ?: BigDecimal.ZERO) - amountInBase
            elements += NetWorthElement(
                name = liability.name,
                currency = liability.currency,
                originalAmount = -liability.currentBalance,
                amountInBaseCurrency = amountInBase.negate().setScale(2, RoundingMode.HALF_UP)
            )
        }

        val total = byCurrency.values.fold(BigDecimal.ZERO) { acc, value -> acc + value }
        val breakdown = byCurrency.entries
            .filter { it.value.signum() > 0 }
            .map { (currency, amount) ->
                val percent = if (total.signum() == 0) {
                    BigDecimal.ZERO
                } else {
                    amount.divide(total, 6, RoundingMode.HALF_UP) * BigDecimal(100)
                }
                NetWorthByCurrency(currency, amount.setScale(2, RoundingMode.HALF_UP), percent)
            }
            .sortedByDescending { it.amountInBaseCurrency }
        val projectedAmount = if (currentMonthBudgetItems.isEmpty()) {
            null
        } else {
            projectedNetWorthDelta(currentMonthBudgetItems, base, rates)?.let { delta ->
                (total + delta).setScale(2, RoundingMode.HALF_UP)
            }
        }
        return NetWorthState(
            currency = base,
            amount = total.setScale(2, RoundingMode.HALF_UP),
            rateDate = if (usedRates) rates?.date else null,
            breakdown = breakdown,
            elements = elements.sortedByDescending { it.amountInBaseCurrency },
            projectedAmount = projectedAmount
        )
    }

    private fun currencyTotals(accounts: List<Account>): List<CurrencyTotal> =
        Currency.entries.mapNotNull { currency ->
            val total = accounts.filter { it.currency == currency }.sumOf { it.balance }
            if (total.signum() == 0) null else CurrencyTotal(currency, total)
        }
}
