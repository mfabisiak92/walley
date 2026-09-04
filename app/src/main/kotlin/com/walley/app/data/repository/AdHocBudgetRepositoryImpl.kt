package com.walley.app.data.repository

import com.walley.app.data.local.AdHocBudgetDao
import com.walley.app.data.local.AdHocBudgetEntity
import com.walley.app.data.local.AdHocBudgetItemEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.AdHocBudgetWithItems
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.effectiveAccountId
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class AdHocBudgetRepositoryImpl @Inject constructor(
    private val dao: AdHocBudgetDao,
    private val accountRepository: AccountRepository,
    private val accountOperationRepository: AccountOperationRepository
) : AdHocBudgetRepository {

    override fun observeAdHocBudgetsWithItems(): Flow<List<AdHocBudgetWithItems>> =
        combine(dao.observeBudgets(), dao.observeAllItems()) { budgets, items ->
            budgets.map { budgetEntity ->
                AdHocBudgetWithItems(
                    budget = budgetEntity.toDomain(),
                    items = items.filter { it.budgetId == budgetEntity.id }.map { it.toDomain() }
                )
            }
        }

    override fun observeAdHocBudget(budgetId: Long): Flow<AdHocBudgetWithItems?> =
        combine(dao.observeBudgetById(budgetId), dao.observeItemsForBudget(budgetId)) { budgetEntity, items ->
            budgetEntity?.let { AdHocBudgetWithItems(it.toDomain(), items.map { item -> item.toDomain() }) }
        }

    override suspend fun saveDraft(
        budgetId: Long?,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long,
        items: List<AdHocBudgetItem>
    ): Long = upsertAdHocBudget(budgetId, name, startDate, endDate, accountId, items, isDraft = true)

    override suspend fun submitBudget(
        budgetId: Long?,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long,
        items: List<AdHocBudgetItem>
    ): Long = upsertAdHocBudget(budgetId, name, startDate, endDate, accountId, items, isDraft = false)

    private suspend fun upsertAdHocBudget(
        budgetId: Long?,
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long,
        items: List<AdHocBudgetItem>,
        isDraft: Boolean
    ): Long {
        val account = accountRepository.observeAccounts().first().first { it.id == accountId }
        val id = if (budgetId != null) {
            dao.updateDetailsAndDraftStatus(budgetId, name, startDate, endDate, accountId, account.currency, isDraft)
            budgetId
        } else {
            dao.insertBudget(
                AdHocBudgetEntity(
                    name = name,
                    startDate = startDate,
                    endDate = endDate,
                    accountId = accountId,
                    currency = account.currency,
                    isDraft = isDraft
                )
            )
        }
        dao.replaceItems(
            id,
            items.map { item ->
                AdHocBudgetItemEntity(
                    budgetId = id,
                    name = item.name,
                    amountMinorUnits = item.amount.toMinorUnits(),
                    icon = item.icon,
                    accountId = item.accountId
                )
            }
        )
        return id
    }

    override suspend fun markItemPaid(itemId: Long) {
        val item = dao.getItem(itemId).toDomain()
        val delta = item.amount - item.paidAmount
        applyAccountDelta(item, delta)
        dao.updateItemPaidAmount(itemId, item.amount.toMinorUnits())
    }

    /** [paidAmount] may exceed the item's planned amount — an item stays free to keep taking payments past what was planned. */
    override suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal) {
        val item = dao.getItem(itemId).toDomain()
        val clamped = paidAmount.coerceAtLeast(BigDecimal.ZERO)
        val delta = clamped - item.paidAmount
        applyAccountDelta(item, delta)
        dao.updateItemPaidAmount(itemId, clamped.toMinorUnits())
    }

    /** Editing the planned amount never touches what's already been paid — an overpayment isn't clawed back by shrinking the plan. */
    override suspend fun updateItemAmount(itemId: Long, amount: BigDecimal) {
        dao.updateItemAmount(itemId, amount.toMinorUnits())
    }

    override suspend fun updateItemIcon(itemId: Long, icon: BudgetItemIcon?) {
        dao.updateItemIcon(itemId, icon)
    }

    override suspend fun deleteBudgetItem(itemId: Long) {
        dao.deleteItem(itemId)
    }

    override suspend fun restoreBudgetItem(item: AdHocBudgetItem) {
        dao.insertItem(
            AdHocBudgetItemEntity(
                id = item.id,
                budgetId = item.budgetId,
                name = item.name,
                amountMinorUnits = item.amount.toMinorUnits(),
                paidAmountMinorUnits = item.paidAmount.toMinorUnits(),
                icon = item.icon,
                accountId = item.accountId
            )
        )
    }

    override suspend fun addBudgetItem(budgetId: Long, name: String, amount: BigDecimal, accountId: Long?, icon: BudgetItemIcon?): Long =
        dao.insertItem(
            AdHocBudgetItemEntity(
                budgetId = budgetId,
                name = name,
                amountMinorUnits = amount.toMinorUnits(),
                icon = icon,
                accountId = accountId
            )
        )

    override suspend fun deleteAdHocBudget(budgetId: Long) {
        val budget = dao.observeBudgetById(budgetId).first()
        if (budget?.isCompleted == true) {
            throw BudgetIsCompletedException()
        }
        dao.deleteItemsForBudget(budgetId)
        dao.deleteBudget(budgetId)
    }

    override suspend fun markCompleted(budgetId: Long) {
        dao.markCompleted(budgetId)
    }

    /**
     * Paying an ad-hoc item withdraws from its own account override, or the budget's default account.
     * @throws InsufficientAccountBalanceException if [delta] is a withdrawal larger than the account's current balance.
     */
    private suspend fun applyAccountDelta(item: AdHocBudgetItem, delta: BigDecimal) {
        if (delta.signum() == 0) return
        val budget = dao.observeBudgetById(item.budgetId).first() ?: return
        val accountId = item.effectiveAccountId(budget.toDomain())
        if (delta.signum() > 0) {
            val account = accountRepository.observeAccounts().first().first { it.id == accountId }
            if (delta > account.balance) {
                throw InsufficientAccountBalanceException(account.name, account.balance, delta, account.currency)
            }
        }
        accountOperationRepository.recordAndApply(accountId, LocalDate.now(), item.name, delta.negate())
    }
}
