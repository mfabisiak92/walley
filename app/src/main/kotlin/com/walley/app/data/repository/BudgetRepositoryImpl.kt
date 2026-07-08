package com.walley.app.data.repository

import com.walley.app.data.local.BudgetDao
import com.walley.app.data.local.BudgetItemEntity
import com.walley.app.data.local.BudgetEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
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
import com.walley.app.domain.model.isAccountWithdrawal
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
    private val snapshotRepository: SnapshotRepository
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
        applyAccountEffects: Boolean
    ): Long = upsertBudget(budgetId, year, month, items, BudgetStatus.DRAFT, applyAccountEffects)

    override suspend fun submitBudget(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        applyAccountEffects: Boolean
    ): Long = upsertBudget(budgetId, year, month, items, BudgetStatus.ACTIVE, applyAccountEffects)

    private suspend fun upsertBudget(
        budgetId: Long?,
        year: Int,
        month: Int,
        items: List<BudgetItem>,
        status: BudgetStatus,
        applyAccountEffects: Boolean
    ): Long {
        val id = if (budgetId != null) {
            budgetDao.updateYearMonthAndStatus(budgetId, year, month, status)
            budgetDao.updateApplyAccountEffects(budgetId, applyAccountEffects)
            budgetId
        } else {
            budgetDao.insertBudget(
                BudgetEntity(year = year, month = month, status = status, applyAccountEffects = applyAccountEffects)
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
        val delta = item.amount - item.paidAmount
        budgetDao.updateItemPaidAmount(itemId, item.amount.toMinorUnits())
        applyAccountDelta(item, delta)
    }

    override suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal) {
        val item = budgetDao.getItem(itemId).toDomain()
        val clamped = paidAmount.coerceIn(BigDecimal.ZERO, item.amount)
        val delta = clamped - item.paidAmount
        budgetDao.updateItemPaidAmount(itemId, clamped.toMinorUnits())
        applyAccountDelta(item, delta)
    }

    override suspend fun updateItemAmount(itemId: Long, amount: BigDecimal) {
        val item = budgetDao.getItem(itemId).toDomain()
        budgetDao.updateItemAmount(itemId, amount.toMinorUnits())
        val clampedPaidAmount = item.paidAmount.coerceAtMost(amount)
        if (clampedPaidAmount != item.paidAmount) {
            budgetDao.updateItemPaidAmount(itemId, clampedPaidAmount.toMinorUnits())
            applyAccountDelta(item, clampedPaidAmount - item.paidAmount)
        }
    }

    override suspend fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?) {
        budgetDao.updateItemIcon(itemId, icon)
    }

    override suspend fun updateItemAccount(itemId: Long, accountId: Long?) {
        val item = budgetDao.getItem(itemId).toDomain()
        if (item.accountId == accountId) return
        if (item.paidAmount.signum() > 0 && accountEffectsEnabledFor(item.budgetId)) {
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
                icon = item.icon
            )
        )
    }

    override suspend fun markBudgetCompleted(budgetId: Long) {
        budgetDao.updateStatus(budgetId, BudgetStatus.COMPLETED)
        captureSnapshot(budgetId)
    }

    override suspend fun updateApplyAccountEffects(budgetId: Long, enabled: Boolean) {
        budgetDao.updateApplyAccountEffects(budgetId, enabled)
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
                if (item.isCompleted || !item.hasPaymentDay) continue

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
                }
            }
        }
    }

    private suspend fun applyAccountDelta(item: BudgetItem, delta: BigDecimal) {
        if (delta.signum() == 0) return
        val accountId = item.accountId ?: return
        if (!accountEffectsEnabledFor(item.budgetId)) return
        applyAccountDeltaForAccount(item.section, accountId, delta)
    }

    private suspend fun accountEffectsEnabledFor(budgetId: Long): Boolean =
        budgetDao.observeBudgetById(budgetId).first()?.applyAccountEffects ?: true

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

        fun accountsTotal(type: AccountType) = accounts
            .filter { it.type == type }
            .fold(BigDecimal.ZERO) { acc, account -> acc + convert(account.balance, account.currency, base, rates) }

        // Net worth counts investment gains after tax, not their pre-tax market value.
        fun accountsNetWorthTotal(type: AccountType) = accounts
            .filter { it.type == type }
            .fold(BigDecimal.ZERO) { acc, account -> acc + convert(account.netWorthValue, account.currency, base, rates) }

        val cashAndChecking = accountsTotal(AccountType.CHECKING) + accountsTotal(AccountType.CASH)
        val savings = accountsTotal(AccountType.SAVING)
        val investments = accountsTotal(AccountType.INVESTMENT)
        val assetsTotal = assets.fold(BigDecimal.ZERO) { acc, asset -> acc + convert(asset.currentValue, asset.currency, base, rates) }
        val liabilitiesTotal = liabilities.fold(BigDecimal.ZERO) { acc, liability ->
            acc + convert(liability.currentBalance, liability.currency, base, rates)
        }
        val netWorth = cashAndChecking + savings + accountsNetWorthTotal(AccountType.INVESTMENT) + assetsTotal - liabilitiesTotal

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

        val investmentContributions = sectionTotal(BudgetSectionType.INVESTMENTS)
        val previous = snapshotRepository.previousSnapshot(budgetEntity.year, budgetEntity.month)
        val investmentGrowth = previous?.let { investments - it.investments - investmentContributions }

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
}
