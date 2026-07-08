package com.walley.app.data.repository

import com.walley.app.data.local.AdHocBudgetDao
import com.walley.app.data.local.AdHocBudgetEntity
import com.walley.app.data.local.AdHocBudgetItemEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.AdHocBudgetItem
import com.walley.app.domain.model.AdHocBudgetWithItems
import com.walley.app.domain.model.BudgetItemIcon
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class AdHocBudgetRepositoryImpl @Inject constructor(
    private val dao: AdHocBudgetDao,
    private val accountRepository: AccountRepository
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

    override suspend fun createAdHocBudget(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long,
        items: List<AdHocBudgetItem>
    ): Long {
        val account = accountRepository.observeAccounts().first().first { it.id == accountId }
        val budgetId = dao.insertBudget(
            AdHocBudgetEntity(
                name = name,
                startDate = startDate,
                endDate = endDate,
                accountId = accountId,
                currency = account.currency
            )
        )
        dao.insertItems(
            items.map { item ->
                AdHocBudgetItemEntity(
                    budgetId = budgetId,
                    name = item.name,
                    amountMinorUnits = item.amount.toMinorUnits(),
                    icon = item.icon
                )
            }
        )
        return budgetId
    }

    override suspend fun markItemPaid(itemId: Long) {
        val item = dao.getItem(itemId).toDomain()
        val delta = item.amount - item.paidAmount
        dao.updateItemPaidAmount(itemId, item.amount.toMinorUnits())
        applyAccountDelta(item, delta)
    }

    override suspend fun markItemPartiallyPaid(itemId: Long, paidAmount: BigDecimal) {
        val item = dao.getItem(itemId).toDomain()
        val clamped = paidAmount.coerceIn(BigDecimal.ZERO, item.amount)
        val delta = clamped - item.paidAmount
        dao.updateItemPaidAmount(itemId, clamped.toMinorUnits())
        applyAccountDelta(item, delta)
    }

    override suspend fun updateItemAmount(itemId: Long, amount: BigDecimal) {
        val item = dao.getItem(itemId).toDomain()
        dao.updateItemAmount(itemId, amount.toMinorUnits())
        val clampedPaidAmount = item.paidAmount.coerceAtMost(amount)
        if (clampedPaidAmount != item.paidAmount) {
            dao.updateItemPaidAmount(itemId, clampedPaidAmount.toMinorUnits())
            applyAccountDelta(item, clampedPaidAmount - item.paidAmount)
        }
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
                icon = item.icon
            )
        )
    }

    override suspend fun deleteAdHocBudget(budgetId: Long) {
        dao.deleteItemsForBudget(budgetId)
        dao.deleteBudget(budgetId)
    }

    /** Paying an ad-hoc item always withdraws from its single linked account. */
    private suspend fun applyAccountDelta(item: AdHocBudgetItem, delta: BigDecimal) {
        if (delta.signum() == 0) return
        val budget = dao.observeBudgetById(item.budgetId).first() ?: return
        accountRepository.addToBalance(budget.accountId, delta.negate())
    }
}
