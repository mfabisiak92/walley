package com.walley.app.data.repository

import com.walley.app.data.local.BudgetDao
import com.walley.app.data.local.BudgetItemEntity
import com.walley.app.data.local.BudgetEntity
import com.walley.app.data.local.toDomain
import com.walley.app.data.local.toMinorUnits
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetWithItems
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val accountRepository: AccountRepository
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

    override fun observeBudget(budgetId: Long): Flow<BudgetWithItems?> =
        combine(
            budgetDao.observeBudgetById(budgetId),
            budgetDao.observeItemsForBudget(budgetId)
        ) { budgetEntity, items ->
            budgetEntity?.let { BudgetWithItems(it.toDomain(), items.map { item -> item.toDomain() }) }
        }

    override suspend fun monthHasBudget(year: Int, month: Int): Boolean =
        budgetDao.countForMonth(year, month) > 0

    override suspend fun createBudget(year: Int, month: Int, items: List<BudgetItem>): Long {
        val budgetId = budgetDao.insertBudget(BudgetEntity(year = year, month = month))
        budgetDao.insertItems(
            items.map { item ->
                BudgetItemEntity(
                    budgetId = budgetId,
                    section = item.section,
                    name = item.name,
                    amountMinorUnits = item.amount.toMinorUnits(),
                    currency = item.currency,
                    accountId = item.accountId,
                    paymentDay = item.paymentDay,
                    paymentDayIsLastOfMonth = item.paymentDayIsLastOfMonth,
                    paidAmountMinorUnits = 0
                )
            }
        )
        return budgetId
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

    override suspend fun deleteBudget(budgetId: Long) {
        val items = budgetDao.observeItemsForBudget(budgetId).first().map { it.toDomain() }
        if (items.any { it.hasAppliedAccountEffect }) {
            throw BudgetHasAppliedItemsException()
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
                paidAmountMinorUnits = item.paidAmount.toMinorUnits()
            )
        )
    }

    override suspend fun checkAndAutoCompleteDueItems() {
        val today = LocalDate.now()
        val budgets = budgetDao.observeBudgets().first()
        val allItems = budgetDao.observeAllItems().first()

        for (budgetEntity in budgets) {
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
        if (item.section == BudgetSectionType.SAVINGS || item.section == BudgetSectionType.INVESTMENTS) {
            accountRepository.addToBalance(accountId, delta)
        }
    }
}
