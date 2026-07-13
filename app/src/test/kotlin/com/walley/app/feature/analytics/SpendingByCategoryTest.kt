package com.walley.app.feature.analytics

import com.walley.app.domain.model.Budget
import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetWithItems
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendingByCategoryTest {

    private val rates = ExchangeRates(
        base = Currency.PLN,
        rates = mapOf(Currency.EUR to BigDecimal("0.25")),
        date = "2026-01-01"
    )

    private fun budget(year: Int, month: Int, items: List<BudgetItem>) =
        BudgetWithItems(Budget(year = year, month = month), items)

    private fun item(
        section: BudgetSectionType,
        amount: String,
        paidAmount: String = amount,
        icon: BudgetItemIcon? = null,
        currency: Currency = Currency.PLN
    ) = BudgetItem(
        section = section,
        name = "Item",
        amount = BigDecimal(amount),
        currency = currency,
        paidAmount = BigDecimal(paidAmount),
        icon = icon
    )

    @Test
    fun `categorySpendHistory groups paid amounts by icon`() {
        val points = categorySpendHistory(
            listOf(
                budget(
                    2026, 1,
                    listOf(
                        item(BudgetSectionType.FIXED_COSTS, "100", icon = BudgetItemIcon.RENT),
                        item(BudgetSectionType.OTHER_COSTS, "50", icon = BudgetItemIcon.GROCERIES),
                        item(BudgetSectionType.FIXED_COSTS, "30", icon = BudgetItemIcon.RENT)
                    )
                )
            ),
            Currency.PLN,
            rates
        )
        assertEquals(1, points.size)
        assertEquals(0, BigDecimal("130").compareTo(points[0].amountsByCategory["Rent"]?.paid))
        assertEquals(0, BigDecimal("50").compareTo(points[0].amountsByCategory["Groceries"]?.paid))
    }

    @Test
    fun `categorySpendHistory sums only FIXED_COSTS and OTHER_COSTS, ignores other sections`() {
        val points = categorySpendHistory(
            listOf(
                budget(
                    2026, 1,
                    listOf(
                        item(BudgetSectionType.FIXED_COSTS, "100", icon = BudgetItemIcon.RENT),
                        item(BudgetSectionType.SAVINGS, "9999", icon = BudgetItemIcon.SAVING),
                        item(BudgetSectionType.INCOME, "9999")
                    )
                )
            ),
            Currency.PLN,
            rates
        )
        assertEquals(setOf("Rent"), points[0].amountsByCategory.keys)
    }

    @Test
    fun `categorySpendHistory uses paidAmount not planned amount`() {
        val points = categorySpendHistory(
            listOf(
                budget(
                    2026, 1,
                    listOf(item(BudgetSectionType.FIXED_COSTS, amount = "200", paidAmount = "80", icon = BudgetItemIcon.RENT))
                )
            ),
            Currency.PLN,
            rates
        )
        assertEquals(0, BigDecimal("80").compareTo(points[0].amountsByCategory["Rent"]?.paid))
        assertEquals(0, BigDecimal("200").compareTo(points[0].amountsByCategory["Rent"]?.planned))
    }

    @Test
    fun `categorySpendHistory excludes one item on a missing rate without nulling the whole month`() {
        val points = categorySpendHistory(
            listOf(
                budget(
                    2026, 1,
                    listOf(
                        item(BudgetSectionType.FIXED_COSTS, "100", icon = BudgetItemIcon.RENT, currency = Currency.GBP),
                        item(BudgetSectionType.OTHER_COSTS, "50", icon = BudgetItemIcon.GROCERIES)
                    )
                )
            ),
            Currency.PLN,
            rates
        )
        assertTrue(points[0].amountsByCategory["Rent"] == null || points[0].amountsByCategory["Rent"]?.paid == BigDecimal.ZERO)
        assertEquals(0, BigDecimal("50").compareTo(points[0].amountsByCategory["Groceries"]?.paid))
    }

    @Test
    fun `categorySpendHistory buckets items with no icon as Uncategorized`() {
        val points = categorySpendHistory(
            listOf(budget(2026, 1, listOf(item(BudgetSectionType.FIXED_COSTS, "40")))),
            Currency.PLN,
            rates
        )
        assertEquals(0, BigDecimal("40").compareTo(points[0].amountsByCategory["Uncategorized"]?.paid))
    }

    @Test
    fun `capToTopCategories keeps top N by total spend and collapses the rest into Other`() {
        val points = listOf(
            CategorySpendPoint(
                java.time.YearMonth.of(2026, 1),
                "Jan '26",
                mapOf(
                    "A" to CategoryAmounts(BigDecimal("100"), BigDecimal("100")),
                    "B" to CategoryAmounts(BigDecimal("80"), BigDecimal("80")),
                    "C" to CategoryAmounts(BigDecimal("10"), BigDecimal("10")),
                    "D" to CategoryAmounts(BigDecimal("5"), BigDecimal("5"))
                )
            )
        )
        val capped = capToTopCategories(points, topN = 2)
        assertEquals(listOf("A", "B", "Other"), capped.categoryLabels)
        assertEquals(100f, capped.seriesByCategory[0][0]!!, 0.001f)
        assertEquals(15f, capped.seriesByCategory[2][0]!!, 0.001f)
    }

    @Test
    fun `capToTopCategories omits Other when there are fewer categories than topN`() {
        val points = listOf(
            CategorySpendPoint(
                java.time.YearMonth.of(2026, 1),
                "Jan '26",
                mapOf("A" to CategoryAmounts(BigDecimal("10"), BigDecimal("10")))
            )
        )
        val capped = capToTopCategories(points, topN = 5)
        assertEquals(listOf("A"), capped.categoryLabels)
    }

    @Test
    fun `varianceForCategory returns 0 not null for a month with no item of that category`() {
        val points = listOf(
            CategorySpendPoint(
                java.time.YearMonth.of(2026, 1),
                "Jan '26",
                mapOf("Rent" to CategoryAmounts(BigDecimal("100"), BigDecimal("100")))
            ),
            CategorySpendPoint(java.time.YearMonth.of(2026, 2), "Feb '26", emptyMap())
        )
        val variance = varianceForCategory(points, "Rent")
        assertEquals(listOf(100f, 0f), variance.planned)
        assertEquals(listOf(100f, 0f), variance.actual)
    }

    @Test
    fun `distinctCategories orders by descending total paid`() {
        val points = listOf(
            CategorySpendPoint(
                java.time.YearMonth.of(2026, 1),
                "Jan '26",
                mapOf(
                    "Small" to CategoryAmounts(BigDecimal("10"), BigDecimal("10")),
                    "Big" to CategoryAmounts(BigDecimal("500"), BigDecimal("500"))
                )
            )
        )
        assertEquals(listOf("Big", "Small"), distinctCategories(points))
    }

    @Test
    fun `categorySpendHistory returns an empty map for a budget with no spending items`() {
        val points = categorySpendHistory(listOf(budget(2026, 1, emptyList())), Currency.PLN, rates)
        assertTrue(points[0].amountsByCategory.isEmpty())
    }

    @Test
    fun `varianceForCategory for an unknown category returns all zeros`() {
        val points = listOf(
            CategorySpendPoint(
                java.time.YearMonth.of(2026, 1),
                "Jan '26",
                mapOf("Rent" to CategoryAmounts(BigDecimal("100"), BigDecimal("100")))
            )
        )
        val variance = varianceForCategory(points, "Nonexistent")
        assertEquals(listOf(0f), variance.planned)
        assertEquals(listOf(0f), variance.actual)
    }
}
