package com.walley.app.data.repository

import com.walley.app.data.local.BudgetDao
import com.walley.app.data.local.BudgetItemEntity
import com.walley.app.data.local.BudgetEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.Account
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import com.walley.app.domain.model.FinancialSnapshot
import com.walley.app.domain.model.IncomeCategory
import com.walley.app.domain.model.InvestmentTransactionType
import com.walley.app.domain.model.accountEffectsGroup
import com.walley.app.domain.model.isAccountWithdrawal
import com.walley.app.domain.model.isFinalizable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val accountRepository: AccountRepository,
    private val assetRepository: AssetRepository,
    private val liabilityRepository: LiabilityRepository,
    private val settingsRepository: SettingsRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val snapshotRepository: SnapshotRepository,
    private val investmentRepository: InvestmentRepository,
    private val accountOperationRepository: AccountOperationRepository
) : BudgetRepository {

    override fun observeBudgetsWithItems(): Flow<List<BudgetWithItems>> =
        combine(budgetDao.observeBudgets(), budgetDao.observeAllItems()) { budgets, items ->
            budgets.map { budgetEntity ->
                BudgetWithItems(
                    budget = budgetEntity.toDomain(),
                    items = items.filter { it.budgetId == budgetEntity.id }.map { it.toDomain() }
                )
            }
        }

    override fun observeBudgetForMonth(year: Int, month: Int): Flow<BudgetWithItems?> =
        combine(budgetDao.observeBudgets(), budgetDao.observeAllItems()) { budgets, items ->
            // A Draft isn't a real budget yet, so it doesn't count as "the budget for this month" here.
            val budgetEntity = budgets.find {
                it.year == year && it.month == month && it.status != BudgetStatus.DRAFT
            } ?: return@combine null
            BudgetWithItems(
                budget = budgetEntity.toDomain(),
                items = items.filter { it.budgetId == budgetEntity.id }.map { it.toDomain() }
            )
        }

    override fun observeBudget(budgetId: Long): Flow<BudgetWithItems?> =
        combine(
            budgetDao.observeBudgetById(budgetId),
            budgetDao.observeItemsForBudget(budgetId)
        ) { budgetEntity, items ->
            budgetEntity?.let { BudgetWithItems(it.toDomain(), items.map { item -> item.toDomain() }) }
        }

    override suspend fun monthHasBudget(year: Int, month: Int, excludeBudgetId: Long?): Boolean =
        budgetDao.countForMonth(year, month, excludeBudgetId) > 0

    override suspend fun saveDraft(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        applyIncomeAccountEffects: Boolean,
        applyCostsAccountEffects: Boolean,
        applySavingsAccountEffects: Boolean,
        applyInvestmentsAccountEffects: Boolean,
        plannedNetWorth: BigDecimal?
    ): Long = upsertBudget(
        budgetId, year, month, items, BudgetStatus.DRAFT,
        applyIncomeAccountEffects, applyCostsAccountEffects, applySavingsAccountEffects, applyInvestmentsAccountEffects,
        plannedNetWorth
    )

    override suspend fun submitBudget(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        applyIncomeAccountEffects: Boolean,
        applyCostsAccountEffects: Boolean,
        applySavingsAccountEffects: Boolean,
        applyInvestmentsAccountEffects: Boolean,
        plannedNetWorth: BigDecimal
    ): Long = upsertBudget(
        budgetId, year, month, items, BudgetStatus.ACTIVE,
        applyIncomeAccountEffects, applyCostsAccountEffects, applySavingsAccountEffects, applyInvestmentsAccountEffects,
        plannedNetWorth
    )

    private suspend fun upsertBudget(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        status: BudgetStatus,
        applyIncomeAccountEffects: Boolean,
        applyCostsAccountEffects: Boolean,
        applySavingsAccountEffects: Boolean,
        applyInvestmentsAccountEffects: Boolean,
        plannedNetWorth: BigDecimal?
    ): Long {
        val id = if (budgetId != null) {
            budgetDao.updateYearMonthAndStatus(budgetId, year, month, status)
            budgetDao.updateApplyIncomeAccountEffects(budgetId, applyIncomeAccountEffects)
            budgetDao.updateApplyCostsAccountEffects(budgetId, applyCostsAccountEffects)
            budgetDao.updateApplySavingsAccountEffects(budgetId, applySavingsAccountEffects)
            budgetDao.updateApplyInvestmentsAccountEffects(budgetId, applyInvestmentsAccountEffects)
            budgetDao.updatePlannedNetWorth(budgetId, plannedNetWorth?.toMinorUnits())
            budgetId
        } else {
            budgetDao.insertBudget(
                BudgetEntity(
                    year = year,
                    month = month,
                    status = status,
                    applyIncomeAccountEffects = applyIncomeAccountEffects,
                    applyCostsAccountEffects = applyCostsAccountEffects,
                    applySavingsAccountEffects = applySavingsAccountEffects,
                    applyInvestmentsAccountEffects = applyInvestmentsAccountEffects,
                    plannedNetWorthMinorUnits = plannedNetWorth?.toMinorUnits()
                )
            )
        }
        budgetDao.replaceItems(
            id,
            items.map { item ->
                BudgetItemEntity(
                    budgetId = id,
                    section = item.section,
                    name = item.name,
                    amountMinorUnits = item.amount.toMinorUnits(),
                    currency = item.currency,
                    accountId = item.accountId,
                    paymentDay = item.paymentDay,
                    paymentDayIsLastOfMonth = item.paymentDayIsLastOfMonth,
                    paidAmountMinorUnits = 0,
                    incomeCategory = item.incomeCategory,
                    icon = item.icon
                )
            }
        )
        return id
    }

    override suspend fun markItemPaid(itemId: Long) {
        val item = budgetDao.getItem(itemId).toDomain()
        if (item.isFinalized) return
        val delta = item.amount - item.paidAmount
        budgetDao.updateItemPaidAmount(itemId, item.amount.toMinorUnits())
        applyAccountDelta(item, delta)
        autoFinalizeIfEligible(item)
    }

    override suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal) {
        val item = budgetDao.getItem(itemId).toDomain()
        if (item.isFinalized) return
        val clamped = paidAmount.coerceIn(BigDecimal.ZERO, item.amount)
        val delta = clamped - item.paidAmount
        budgetDao.updateItemPaidAmount(itemId, clamped.toMinorUnits())
        applyAccountDelta(item, delta)
    }

    override suspend fun updateItemAmount(itemId: Long, amount: BigDecimal) {
        val item = budgetDao.getItem(itemId).toDomain()
        if (item.isFinalized) return
        budgetDao.updateItemAmount(itemId, amount.toMinorUnits())
        val clampedPaidAmount = item.paidAmount.coerceAtMost(amount)
        if (clampedPaidAmount != item.paidAmount) {
            budgetDao.updateItemPaidAmount(itemId, clampedPaidAmount.toMinorUnits())
            applyAccountDelta(item, clampedPaidAmount - item.paidAmount)
        }
    }

    override suspend fun finalizeItem(itemId: Long) {
        val item = budgetDao.getItem(itemId).toDomain()
        if (!item.section.isFinalizable || item.isFinalized) return
        budgetDao.finalizeItem(itemId)
    }

    /** Marking an Income/Income-related-expenses item fully paid locks it in as final automatically. */
    private suspend fun autoFinalizeIfEligible(item: BudgetItem) {
        if (item.section.isFinalizable) budgetDao.finalizeItem(item.id)
    }

    override suspend fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?) {
        budgetDao.updateItemIcon(itemId, icon)
    }

    override suspend fun updateItemAccount(itemId: Long, accountId: Long?) {
        val item = budgetDao.getItem(itemId).toDomain()
        if (item.accountId == accountId) return
        if (item.paidAmount.signum() > 0 && accountEffectsEnabledFor(item.budgetId, item.section)) {
            item.accountId?.let { oldAccountId ->
                applyAccountDeltaForAccount(item.section, oldAccountId, item.paidAmount.negate())
            }
            accountId?.let { newAccountId ->
                applyAccountDeltaForAccount(item.section, newAccountId, item.paidAmount)
            }
        }
        // Savings/Investments items are defined by their account — its name and currency come along with it.
        val isAccountLinkedSection = item.section == BudgetSectionType.SAVINGS ||
            item.section == BudgetSectionType.INVESTMENTS
        val newAccount = accountId?.let { id -> accountRepository.observeAccounts().first().find { it.id == id } }
        if (isAccountLinkedSection && newAccount != null) {
            budgetDao.updateItemAccountNameAndCurrency(itemId, newAccount.id, newAccount.name, newAccount.currency)
        } else {
            budgetDao.updateItemAccount(itemId, accountId)
        }
    }

    override suspend fun deleteBudget(budgetId: Long) {
        val budget = budgetDao.observeBudgetById(budgetId).first()
        if (budget?.status == BudgetStatus.COMPLETED) {
            throw BudgetIsCompletedException()
        }
        budgetDao.deleteItemsForBudget(budgetId)
        budgetDao.deleteBudget(budgetId)
    }

    override suspend fun deleteBudgetItem(itemId: Long) {
        budgetDao.deleteItem(itemId)
    }

    override suspend fun restoreBudgetItem(item: BudgetItem) {
        budgetDao.insertItem(
            BudgetItemEntity(
                id = item.id,
                budgetId = item.budgetId,
                section = item.section,
                name = item.name,
                amountMinorUnits = item.amount.toMinorUnits(),
                currency = item.currency,
                accountId = item.accountId,
                paymentDay = item.paymentDay,
                paymentDayIsLastOfMonth = item.paymentDayIsLastOfMonth,
                paidAmountMinorUnits = item.paidAmount.toMinorUnits(),
                incomeCategory = item.incomeCategory,
                icon = item.icon,
                isFinalized = item.isFinalized
            )
        )
    }

    override suspend fun addBudgetItem(
        budgetId: Long,
        section: BudgetSectionType,
        name: String,
        amount: BigDecimal,
        currency: Currency,
        accountId: Long?,
        paymentDay: Int?,
        paymentDayIsLastOfMonth: Boolean,
        incomeCategory: IncomeCategory?,
        icon: BudgetItemIcon?
    ): Long = budgetDao.insertItem(
        BudgetItemEntity(
            budgetId = budgetId,
            section = section,
            name = name,
            amountMinorUnits = amount.toMinorUnits(),
            currency = currency,
            accountId = accountId,
            paymentDay = paymentDay,
            paymentDayIsLastOfMonth = paymentDayIsLastOfMonth,
            paidAmountMinorUnits = 0,
            incomeCategory = incomeCategory,
            icon = icon
        )
    )

    override suspend fun markBudgetCompleted(budgetId: Long) {
        budgetDao.updateStatus(budgetId, BudgetStatus.COMPLETED)
        captureSnapshot(budgetId)
    }

    override suspend fun updateApplyIncomeAccountEffects(budgetId: Long, enabled: Boolean) {
        budgetDao.updateApplyIncomeAccountEffects(budgetId, enabled)
    }

    override suspend fun updateApplyCostsAccountEffects(budgetId: Long, enabled: Boolean) {
        budgetDao.updateApplyCostsAccountEffects(budgetId, enabled)
    }

    override suspend fun updateApplySavingsAccountEffects(budgetId: Long, enabled: Boolean) {
        budgetDao.updateApplySavingsAccountEffects(budgetId, enabled)
    }

    override suspend fun updateApplyInvestmentsAccountEffects(budgetId: Long, enabled: Boolean) {
        budgetDao.updateApplyInvestmentsAccountEffects(budgetId, enabled)
    }

    override suspend fun updatePlannedNetWorth(budgetId: Long, plannedNetWorth: BigDecimal) {
        budgetDao.updatePlannedNetWorth(budgetId, plannedNetWorth.toMinorUnits())
    }

    override suspend fun checkAndAutoCompleteDueItems() {
        val today = LocalDate.now()
        val budgets = budgetDao.observeBudgets().first()
        val allItems = budgetDao.observeAllItems().first()

        for (budgetEntity in budgets) {
            if (budgetEntity.status != BudgetStatus.ACTIVE) continue
            val yearMonth = YearMonth.of(budgetEntity.year, budgetEntity.month)
            for (itemEntity in allItems) {
                if (itemEntity.budgetId != budgetEntity.id) continue
                val item = itemEntity.toDomain()
                if (item.isCompleted || item.isFinalized || !item.hasPaymentDay) continue

                val targetDay = if (item.paymentDayIsLastOfMonth) {
                    yearMonth.lengthOfMonth()
                } else {
                    item.paymentDay!!.coerceAtMost(yearMonth.lengthOfMonth())
                }
                val targetDate = yearMonth.atDay(targetDay)

                if (!today.isBefore(targetDate)) {
                    val delta = item.amount - item.paidAmount
                    budgetDao.updateItemPaidAmount(item.id, item.amount.toMinorUnits())
                    applyAccountDelta(item, delta)
                    autoFinalizeIfEligible(item)
                }
            }
        }
    }

    private suspend fun applyAccountDelta(item: BudgetItem, delta: BigDecimal) {
        if (delta.signum() == 0) return
        val accountId = item.accountId ?: return
        if (!accountEffectsEnabledFor(item.budgetId, item.section)) return
        applyAccountDeltaForAccount(item.section, accountId, delta)
    }

    private suspend fun accountEffectsEnabledFor(budgetId: Long, section: BudgetSectionType): Boolean =
        budgetDao.observeBudgetById(budgetId).first()?.toDomain()?.accountEffectsEnabled(section.accountEffectsGroup) ?: true

    private suspend fun applyAccountDeltaForAccount(section: BudgetSectionType, accountId: Long, delta: BigDecimal) {
        if (delta.signum() == 0) return
        when {
            section.isAccountWithdrawal -> accountRepository.addToBalance(accountId, delta.negate())
            section == BudgetSectionType.SAVINGS || section == BudgetSectionType.INVESTMENTS ||
                section == BudgetSectionType.INCOME -> accountRepository.addToBalance(accountId, delta)
            else -> Unit
        }
    }

    /**
     * Converts [amount] into [target]; falls back to zero (rather than failing budget completion,
     * which must always succeed) if a needed exchange rate isn't available.
     */
    private fun convert(amount: BigDecimal, currency: Currency, target: Currency, rates: ExchangeRates?): BigDecimal {
        if (currency == target) return amount
        val rate = rates?.rates?.get(currency) ?: return BigDecimal.ZERO
        return amount.divide(rate, 6, RoundingMode.HALF_UP)
    }

    private suspend fun captureSnapshot(budgetId: Long) {
        val budgetEntity = budgetDao.observeBudgetById(budgetId).first() ?: return
        val items = budgetDao.observeItemsForBudget(budgetId).first().map { it.toDomain() }
        val base = settingsRepository.observeBaseCurrency().first()
        val rates = exchangeRateRepository.observeRates(base).first()
        val accounts = accountRepository.observeAccounts().first()
        val assets = assetRepository.observeAssets().first()
        val liabilities = liabilityRepository.observeLiabilities().first()
        val includeSavingsInNetWorth = settingsRepository.observeIncludeSavingsInNetWorth().first()

        // Virtual accounts' balance already sits in a real account, and closed accounts no longer count,
        // so both are excluded from every total to avoid double-counting/stale figures.
        fun accountsTotal(type: AccountType) = accounts
            .filter { it.type == type && !it.isVirtual && !it.isClosed }
            .fold(BigDecimal.ZERO) { acc, account -> acc + convert(account.balance, account.currency, base, rates) }

        val cashAndChecking = accountsTotal(AccountType.CHECKING) + accountsTotal(AccountType.CASH)
        val savings = accountsTotal(AccountType.SAVING)
        val investments = accountsTotal(AccountType.INVESTMENT)
        val assetsTotal = assets.fold(BigDecimal.ZERO) { acc, asset -> acc + convert(asset.currentValue, asset.currency, base, rates) }
        val liabilitiesTotal = liabilities.fold(BigDecimal.ZERO) { acc, liability ->
            acc + convert(liability.currentBalance, liability.currency, base, rates)
        }
        // Computed straight from Account.netWorthContribution (not from cashAndChecking/savings above)
        // since a virtual Saving account's earmarked balance has to be subtracted back out of its
        // host account when savings are excluded — see Account.netWorthContribution for why.
        val accountsNetWorth = accounts.fold(BigDecimal.ZERO) { acc, account ->
            val contribution = account.netWorthContribution(includeSavingsInNetWorth)
            if (contribution.signum() == 0) acc else acc + convert(contribution, account.currency, base, rates)
        }
        val netWorth = accountsNetWorth + assetsTotal - liabilitiesTotal

        fun sectionTotal(section: BudgetSectionType) = items
            .filter { it.section == section }
            .fold(BigDecimal.ZERO) { acc, item -> acc + convert(item.amount, item.currency, base, rates) }

        val income = sectionTotal(BudgetSectionType.INCOME)
        val incomeRelatedExpenses = sectionTotal(BudgetSectionType.INCOME_RELATED_EXPENSES)
        val disposableIncome = income - incomeRelatedExpenses

        val incomeItems = items.filter { it.section == BudgetSectionType.INCOME }
        fun categoryTotal(category: IncomeCategory) = incomeItems
            .filter { it.incomeCategory == category }
            .fold(BigDecimal.ZERO) { acc, item -> acc + convert(item.amount, item.currency, base, rates) }

        val salaryIncome = categoryTotal(IncomeCategory.SALARY)
        val dividendsIncome = categoryTotal(IncomeCategory.DIVIDENDS)
        val interestIncome = categoryTotal(IncomeCategory.INTEREST)
        // Covers items explicitly tagged OTHER, plus any legacy items with no category at all.
        val otherIncome = income - salaryIncome - dividendsIncome - interestIncome

        // Money that entered investments this month, from three distinct sources that all inflate
        // the total value without being market growth: budgeted transfers into the account (cash
        // that may still be sitting uninvested), actual buy/sell events (which move a position's
        // value directly, regardless of how the account was funded), and imported cash operations
        // (deposits/withdrawals/interest from a CSV import) — the only one of the three that already
        // updates the account's stored balance directly, which is exactly why it has to be subtracted
        // here too, or it would otherwise get counted as if it were market growth.
        val investmentContributions = sectionTotal(BudgetSectionType.INVESTMENTS)
        val investmentTransactionContributions = investmentTransactionNetContributions(budgetEntity.year, budgetEntity.month, base, rates)
        val accountOperationContributions =
            accountOperationNetContributions(budgetEntity.year, budgetEntity.month, accounts, base, rates)
        val previous = snapshotRepository.previousSnapshot(budgetEntity.year, budgetEntity.month)
        val investmentGrowth = previous?.let {
            investments - it.investments - investmentContributions - investmentTransactionContributions - accountOperationContributions
        }

        snapshotRepository.addSnapshot(
            FinancialSnapshot(
                budgetId = budgetId,
                year = budgetEntity.year,
                month = budgetEntity.month,
                baseCurrency = base,
                cashAndChecking = cashAndChecking.setScale(2, RoundingMode.HALF_UP),
                savings = savings.setScale(2, RoundingMode.HALF_UP),
                investments = investments.setScale(2, RoundingMode.HALF_UP),
                assets = assetsTotal.setScale(2, RoundingMode.HALF_UP),
                liabilities = liabilitiesTotal.setScale(2, RoundingMode.HALF_UP),
                netWorth = netWorth.setScale(2, RoundingMode.HALF_UP),
                income = income.setScale(2, RoundingMode.HALF_UP),
                incomeRelatedExpenses = incomeRelatedExpenses.setScale(2, RoundingMode.HALF_UP),
                disposableIncome = disposableIncome.setScale(2, RoundingMode.HALF_UP),
                salaryIncome = salaryIncome.setScale(2, RoundingMode.HALF_UP),
                dividendsIncome = dividendsIncome.setScale(2, RoundingMode.HALF_UP),
                interestIncome = interestIncome.setScale(2, RoundingMode.HALF_UP),
                otherIncome = otherIncome.setScale(2, RoundingMode.HALF_UP),
                investmentGrowth = investmentGrowth?.setScale(2, RoundingMode.HALF_UP)
            )
        )
    }

    /** Net of this calendar month's buy/sell events across every investment, converted to [base]. */
    private suspend fun investmentTransactionNetContributions(
        year: Int,
        month: Int,
        base: Currency,
        rates: ExchangeRates?
    ): BigDecimal {
        val yearMonth = YearMonth.of(year, month)
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        return investmentRepository.observeInvestments().first().fold(BigDecimal.ZERO) { acc, data ->
            val net = data.transactions
                .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
                .fold(BigDecimal.ZERO) { sum, transaction ->
                    when (transaction.type) {
                        InvestmentTransactionType.BUY -> sum + transaction.netAmount
                        InvestmentTransactionType.SELL -> sum - transaction.netAmount
                    }
                }
            acc + convert(net, data.investment.currency, base, rates)
        }
    }

    /** Net of this calendar month's imported account operations across investment accounts, converted to [base]. */
    private suspend fun accountOperationNetContributions(
        year: Int,
        month: Int,
        accounts: List<Account>,
        base: Currency,
        rates: ExchangeRates?
    ): BigDecimal {
        val yearMonth = YearMonth.of(year, month)
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth()
        val currencyByAccountId = accounts.associate { it.id to it.currency }
        return accountOperationRepository.observeAll().first()
            .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
            .fold(BigDecimal.ZERO) { acc, operation ->
                val currency = currencyByAccountId[operation.accountId] ?: return@fold acc
                acc + convert(operation.amount, currency, base, rates)
            }
    }
}
