package com.walley.app.feature.budget

import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetSurplusTest {

    private fun item(
        section: BudgetSectionType,
        amount: String,
        paidAmount: String,
        currency: Currency = Currency.PLN,
        icon: BudgetItemIcon? = null
    ) = BudgetItem(
        section = section,
        name = "Item",
        amount = BigDecimal(amount),
        currency = currency,
        paidAmount = BigDecimal(paidAmount),
        icon = icon
    )

    @Test
    fun `sums underspend per icon category, ignoring overspent and fully-spent items`() {
        val result = budgetSurplus(
            listOf(
                item(BudgetSectionType.FIXED_COSTS, "100", "70", icon = BudgetItemIcon.RENT),
                item(BudgetSectionType.OTHER_COSTS, "50", "50", icon = BudgetItemIcon.GROCERIES),
                item(BudgetSectionType.FIXED_COSTS, "40", "60", icon = BudgetItemIcon.CAR)
            ),
            uncategorizedLabel = "Uncategorized"
        )
        assertEquals(1, result.size)
        assertEquals(0, BigDecimal("30").compareTo(result[0].total))
        assertEquals(listOf(SurplusCategory("Rent", BigDecimal("30"))), result[0].categories)
    }

    @Test
    fun `ignores sections other than Fixed and Other costs`() {
        val result = budgetSurplus(
            listOf(
                item(BudgetSectionType.SAVINGS, "100", "20", icon = BudgetItemIcon.SAVING),
                item(BudgetSectionType.INCOME, "100", "20")
            ),
            uncategorizedLabel = "Uncategorized"
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `items with no icon fall back to the uncategorized label`() {
        val result = budgetSurplus(
            listOf(item(BudgetSectionType.FIXED_COSTS, "40", "10")),
            uncategorizedLabel = "Uncategorized"
        )
        assertEquals(listOf(SurplusCategory("Uncategorized", BigDecimal("30"))), result[0].categories)
    }

    @Test
    fun `pools leftover strictly per currency with no conversion`() {
        val result = budgetSurplus(
            listOf(
                item(BudgetSectionType.FIXED_COSTS, "100", "60", currency = Currency.PLN, icon = BudgetItemIcon.RENT),
                item(BudgetSectionType.OTHER_COSTS, "50", "10", currency = Currency.EUR, icon = BudgetItemIcon.GROCERIES)
            ),
            uncategorizedLabel = "Uncategorized"
        )
        val byCurrency = result.associateBy { it.currency }
        assertEquals(0, BigDecimal("40").compareTo(byCurrency.getValue(Currency.PLN).total))
        assertEquals(0, BigDecimal("40").compareTo(byCurrency.getValue(Currency.EUR).total))
    }

    @Test
    fun `returns an empty list when nothing is left over`() {
        val result = budgetSurplus(
            listOf(item(BudgetSectionType.FIXED_COSTS, "100", "100", icon = BudgetItemIcon.RENT)),
            uncategorizedLabel = "Uncategorized"
        )
        assertTrue(result.isEmpty())
    }
}
