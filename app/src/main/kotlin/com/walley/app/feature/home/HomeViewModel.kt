package com.walley.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AssetRepository
import com.walley.app.data.repository.BackupWarningRepository
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.LiabilityRepository
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.data.repository.SnapshotRepository
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
import com.walley.app.feature.analytics.findByYearMonth
import com.walley.app.feature.budget.BudgetProgress
import com.walley.app.feature.budget.ProjectedNetWorthBreakdown
import com.walley.app.feature.budget.SPENDING_SECTIONS
import com.walley.app.feature.budget.budgetProgress
import com.walley.app.feature.budget.convertToCurrency
import com.walley.app.feature.budget.projectedNetWorthBreakdown
import com.walley.app.feature.budget.unallocatedAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

data class HomeBalances(
    /** Balance of non-virtual [AccountType.CHECKING]/[AccountType.CASH] accounts, summed per currency. */
    val checkingCash: List<CurrencyTotal> = emptyList(),
    val savings: List<CurrencyTotal> = emptyList(),
    /**
     * [checkingCash] with any virtual Saving envelope's earmark subtracted back out — what's actually
     * free to spend. A virtual account's balance already physically sits inside its (non-virtual)
     * host's balance, so this can't be computed per-account; instead, mirroring
     * [Account.netWorthContribution], the earmarked amount is subtracted from the flat currency total
     * rather than from a specific host account.
     */
    val availableBalance: List<CurrencyTotal> = emptyList(),
    val investments: List<CurrencyTotal> = emptyList(),
    /** Same as [investments], but with tax owed on any unrealized gain subtracted per account. */
    val investmentsAfterTax: List<CurrencyTotal> = emptyList(),
    /** Current value minus cost basis, summed per currency — used to color the Investments tile. */
    val investmentsGainLoss: List<CurrencyTotal> = emptyList(),
    /** Current market value of positions held (i.e. [investments] minus [uninvestedCash]), summed per currency. */
    val investedAmount: List<CurrencyTotal> = emptyList(),
    /** Cash sitting in investment accounts that hasn't been put into a position yet, summed per currency. */
    val uninvestedCash: List<CurrencyTotal> = emptyList()
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
    val projectedAmount: BigDecimal? = null,
    // the terms behind projectedAmount (income, income-related expenses, fixed/other costs, savings
    // adjustment), for showing the projection's math on the breakdown screen; null under the same
    // conditions as projectedAmount
    val projectedBreakdown: ProjectedNetWorthBreakdown? = null,
    // net worth as of the end of the previous calendar month, from that month's financial snapshot;
    // null when the previous month's budget was never marked completed (no snapshot was ever taken)
    val previousMonthNetWorth: BigDecimal? = null
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
    investmentRepository: InvestmentRepository,
    snapshotRepository: SnapshotRepository,
    backupWarningRepository: BackupWarningRepository
) : ViewModel() {

    val showBackupWarning: StateFlow<Boolean> = backupWarningRepository.shouldShowBackupWarning()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val homeBalances: StateFlow<HomeBalances> = accountRepository.observeAccounts()
        .map { accounts ->
            val investmentAccounts = accounts.filter { it.type == AccountType.INVESTMENT }
            val checkingCashAccounts = accounts.filter { !it.isVirtual && (it.type == AccountType.CHECKING || it.type == AccountType.CASH) }
            // A virtual Saving envelope's money already sits inside a real (non-virtual) account's balance,
            // so subtracting its own balance back out of the flat total has the same effect as subtracting
            // it from whichever specific account it's earmarked from, without needing to know which one.
            // Closed envelopes are excluded: closing one sweeps its balance back into its host (see
            // AccountRepositoryImpl.closeAccount), so it no longer earmarks anything.
            val virtualSavingsAccounts = accounts.filter { it.isVirtual && it.type == AccountType.SAVING && !it.isClosed }
            val checkingCashTotals = currencyTotals(checkingCashAccounts)
            HomeBalances(
                checkingCash = checkingCashTotals,
                savings = currencyTotals(accounts.filter { it.type == AccountType.SAVING && !it.isClosed }),
                // Derived from checkingCashTotals (rather than currencyTotals on the combined account list)
                // so a currency whose envelopes exactly exhaust its checking/cash balance still gets an
                // entry showing zero, instead of being dropped and falling back to the un-deducted total.
                availableBalance = checkingCashTotals.map { (currency, total) ->
                    val earmarked = virtualSavingsAccounts.filter { it.currency == currency }.sumOf { it.balance }
                    CurrencyTotal(currency, total - earmarked)
                },
                investments = currencyTotals(investmentAccounts),
                investmentsAfterTax = currencyTotals(investmentAccounts) { it.netWorthValue },
                investmentsGainLoss = currencyTotals(investmentAccounts) { it.investmentGainLoss },
                investedAmount = currencyTotals(investmentAccounts) { it.balance - it.uninvestedCash },
                uninvestedCash = currencyTotals(investmentAccounts) { it.uninvestedCash }
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

    /** From the previous calendar month's financial snapshot, if that month's budget was ever marked completed. */
    private val previousMonthNetWorth: Flow<BigDecimal?> = snapshotRepository.observeSnapshots()
        .map { snapshots -> findByYearMonth(snapshots, YearMonth.now().minusMonths(1)) { it.yearMonth }?.netWorth }

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
    }.combine(previousMonthNetWorth) { state, previous ->
        state?.copy(previousMonthNetWorth = previous)
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
        val projectedBreakdown = if (currentMonthBudgetItems.isEmpty()) {
            null
        } else {
            projectedNetWorthBreakdown(currentMonthBudgetItems, accounts, base, rates, includeSavings)
        }
        val projectedAmount = projectedBreakdown?.let { (total + it.total).setScale(2, RoundingMode.HALF_UP) }
        return NetWorthState(
            currency = base,
            amount = total.setScale(2, RoundingMode.HALF_UP),
            rateDate = if (usedRates) rates?.date else null,
            breakdown = breakdown,
            elements = elements.sortedByDescending { it.amountInBaseCurrency },
            projectedAmount = projectedAmount,
            projectedBreakdown = projectedBreakdown
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
