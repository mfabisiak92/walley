package com.walley.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AssetRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.InvestmentRepository
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
import com.walley.app.domain.model.estimatedTaxByYear
import com.walley.app.feature.budget.BudgetProgress
import com.walley.app.feature.budget.SPENDING_SECTIONS
import com.walley.app.feature.budget.budgetProgress
import com.walley.app.feature.budget.convertToCurrency
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class HomeBalances(
    val total: List<CurrencyTotal> = emptyList(),
    val savings: List<CurrencyTotal> = emptyList(),
    val investments: List<CurrencyTotal> = emptyList(),
    /** Same as [investments], but with tax owed on any unrealized gain subtracted per account. */
    val investmentsAfterTax: List<CurrencyTotal> = emptyList(),
    /** Current value minus cost basis, summed per currency — used to color the Investments tile. */
    val investmentsGainLoss: List<CurrencyTotal> = emptyList()
)

data class NetWorthByCurrency(
    val currency: Currency,
    val amountInBaseCurrency: BigDecimal,
    val percent: BigDecimal
)

/** Grouping used to collapse the breakdown screen's element list by kind. */
enum class NetWorthCategory(val label: String) {
    CHECKING("Checking accounts"),
    SAVING("Saving accounts"),
    CASH("Cash accounts"),
    INVESTMENT("Investment accounts"),
    ASSET("Assets"),
    LIABILITY("Liabilities")
}

/** One contributor to net worth (an account or an asset), shown in the breakdown screen. */
data class NetWorthElement(
    val name: String,
    val category: NetWorthCategory,
    val currency: Currency,
    val originalAmount: BigDecimal,
    val amountInBaseCurrency: BigDecimal
)

private fun AccountType.toNetWorthCategory(): NetWorthCategory = when (this) {
    AccountType.CHECKING -> NetWorthCategory.CHECKING
    AccountType.SAVING -> NetWorthCategory.SAVING
    AccountType.CASH -> NetWorthCategory.CASH
    AccountType.INVESTMENT -> NetWorthCategory.INVESTMENT
}

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
    exchangeRateRepository: ExchangeRateRepository,
    investmentRepository: InvestmentRepository
) : ViewModel() {

    val homeBalances: StateFlow<HomeBalances> = accountRepository.observeAccounts()
        .map { accounts ->
            val investmentAccounts = accounts.filter { it.type == AccountType.INVESTMENT }
            HomeBalances(
                // Virtual accounts' balance already sits in a real account, so excluding them here avoids double-counting.
                total = currencyTotals(accounts.filterNot { it.isVirtual }),
                savings = currencyTotals(accounts.filter { it.type == AccountType.SAVING && !it.isClosed }),
                investments = currencyTotals(investmentAccounts),
                investmentsAfterTax = currencyTotals(investmentAccounts) { it.netWorthValue },
                investmentsGainLoss = currencyTotals(investmentAccounts) { it.investmentGainLoss }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeBalances())

    private val baseCurrencyRates = settingsRepository.observeBaseCurrency()
        .flatMapLatest { base ->
            exchangeRateRepository.observeRates(base).map { rates -> base to rates }
        }

    private val investmentsByAccount = investmentRepository.observeInvestments()
        .map { investments -> investments.filter { it.investment.accountId != null }.groupBy { it.investment.accountId!! } }

    init {
        // Keeps a "Tax {year}" liability in sync with the live realized-gain estimate
        // for every year that owes anything, so it's automatically part of net worth everywhere.
        combine(
            accountRepository.observeAccounts(),
            investmentsByAccount,
            baseCurrencyRates
        ) { accounts, byAccount, (base, rates) ->
            val amountsByYear = estimatedTaxByYear(accounts, byAccount) { amount, currency ->
                convertToCurrency(amount, currency, base, rates) ?: BigDecimal.ZERO
            }
            base to amountsByYear
        }
            .onEach { (base, amountsByYear) -> liabilityRepository.syncEstimatedTaxLiabilities(amountsByYear, base) }
            .launchIn(viewModelScope)
    }

    private val currentMonthBudget = LocalDate.now().let { today ->
        budgetRepository.observeBudgetForMonth(today.year, today.monthValue)
    }

    private val currentMonthBudgetItems = currentMonthBudget.map { it?.items ?: emptyList() }

    private val baseCurrencyRatesAndNetWorthSettings = combine(
        baseCurrencyRates,
        settingsRepository.observeIncludeSavingsInNetWorth()
    ) { (base, rates), includeSavings -> Triple(base, rates, includeSavings) }

    val netWorth: StateFlow<NetWorthState?> = combine(
        accountRepository.observeAccounts(),
        assetRepository.observeAssets(),
        liabilityRepository.observeLiabilities(),
        baseCurrencyRatesAndNetWorthSettings,
        currentMonthBudgetItems
    ) { accounts, assets, liabilities, (base, rates, includeSavings), budgetItems ->
        if (accounts.isEmpty() && assets.isEmpty() && liabilities.isEmpty()) {
            null
        } else {
            computeNetWorth(accounts, assets, liabilities, budgetItems, base, rates, includeSavings)
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
        rates: ExchangeRates?,
        includeSavings: Boolean
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
            // See Account.netWorthContribution: usually the account's netWorthValue, but a virtual
            // Saving account contributes nothing (already counted via its host) when savings are
            // included, or its earmarked amount back out (negative) when savings are excluded.
            val contribution = account.netWorthContribution(includeSavings)
            if (contribution.signum() == 0) continue
            val amountInBase = convertToBase(contribution, account.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[account.currency] = (byCurrency[account.currency] ?: BigDecimal.ZERO) + amountInBase
            elements += NetWorthElement(
                name = account.name,
                category = account.type.toNetWorthCategory(),
                currency = account.currency,
                originalAmount = contribution,
                amountInBaseCurrency = amountInBase.setScale(2, RoundingMode.HALF_UP)
            )
        }
        for (asset in assets) {
            val amountInBase = convertToBase(asset.currentValue, asset.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[asset.currency] = (byCurrency[asset.currency] ?: BigDecimal.ZERO) + amountInBase
            elements += NetWorthElement(
                name = asset.name,
                category = NetWorthCategory.ASSET,
                currency = asset.currency,
                originalAmount = asset.currentValue,
                amountInBaseCurrency = amountInBase.setScale(2, RoundingMode.HALF_UP)
            )
        }
        // A fully paid-off liability owes nothing, so it no longer counts against net worth.
        for (liability in liabilities.filter { it.currentBalance.signum() != 0 }) {
            val amountInBase = convertToBase(liability.currentBalance, liability.currency)
                ?: return NetWorthState(currency = base, amount = null, rateDate = null)
            byCurrency[liability.currency] = (byCurrency[liability.currency] ?: BigDecimal.ZERO) - amountInBase
            elements += NetWorthElement(
                name = liability.name,
                category = NetWorthCategory.LIABILITY,
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

    private fun currencyTotals(
        accounts: List<Account>,
        amount: (Account) -> BigDecimal = { it.balance }
    ): List<CurrencyTotal> =
        Currency.entries.mapNotNull { currency ->
            val total = accounts.filter { it.currency == currency }.sumOf(amount)
            if (total.signum() == 0) null else CurrencyTotal(currency, total)
        }
}
