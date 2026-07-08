package com.walley.app.feature.budget

import com.walley.app.domain.model.BudgetItem
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.ExchangeRates
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetProgressTest {

    private val rates = ExchangeRates(
        base = Currency.PLN,
        rates = mapOf(Currency.EUR to BigDecimal("0.25"), Currency.USD to BigDecimal("0.27")),
        date = "2026-01-01"
    )

    private fun item(
        section: BudgetSectionType,
        amount: String,
        currency: Currency = Currency.PLN,
        paidAmount: String = "0"
    ) = BudgetItem(
        section = section,
        name = "Item",
        amount = BigDecimal(amount),
        currency = currency,
        paidAmount = BigDecimal(paidAmount)
    )

    @Test
    fun `convertToCurrency is a no-op in the same currency`() {
        assertEquals(BigDecimal("100"), convertToCurrency(BigDecimal("100"), Currency.PLN, Currency.PLN, rates))
    }

    @Test
    fun `convertToCurrency divides by the target's rate`() {
        // 100 EUR at rate 0.25 (1 PLN = 0.25 EUR) -> 400 PLN
        val result = convertToCurrency(BigDecimal("100"), Currency.EUR, Currency.PLN, rates)
        assertEquals(0, BigDecimal("400").compareTo(result))
    }

    @Test
    fun `convertToCurrency returns null when the rate is missing`() {
        assertNull(convertToCurrency(BigDecimal("100"), Currency.GBP, Currency.PLN, rates))
    }

    @Test
    fun `convertToCurrency returns null when rates are null and currencies differ`() {
        assertNull(convertToCurrency(BigDecimal("100"), Currency.EUR, Currency.PLN, null))
    }

    @Test
    fun `sectionTotal sums only items in that section`() {
        val items = listOf(
            item(BudgetSectionType.FIXED_COSTS, "100"),
            item(BudgetSectionType.FIXED_COSTS, "50"),
            item(BudgetSectionType.OTHER_COSTS, "999")
        )
        assertEquals(BigDecimal("150"), sectionTotal(items, BudgetSectionType.FIXED_COSTS, Currency.PLN, rates))
    }

    @Test
    fun `sectionTotal returns null when a needed rate is missing`() {
        val items = listOf(item(BudgetSectionType.FIXED_COSTS, "100", currency = Currency.GBP))
        assertNull(sectionTotal(items, BudgetSectionType.FIXED_COSTS, Currency.PLN, rates))
    }

    @Test
    fun `disposableIncome subtracts income-related expenses from income`() {
        val items = listOf(
            item(BudgetSectionType.INCOME, "5000"),
            item(BudgetSectionType.INCOME_RELATED_EXPENSES, "500")
        )
        assertEquals(BigDecimal("4500"), disposableIncome(items, Currency.PLN, rates))
    }

    @Test
    fun `budgetProgress percent is zero when nothing is planned`() {
        val progress = budgetProgress(emptyList(), setOf(BudgetSectionType.FIXED_COSTS), Currency.PLN, rates)
        assertEquals(BigDecimal.ZERO, progress?.percent)
    }

    @Test
    fun `budgetProgress computes spent over planned as a percentage`() {
        val items = listOf(item(BudgetSectionType.FIXED_COSTS, "200", paidAmount = "50"))
        val progress = budgetProgress(items, setOf(BudgetSectionType.FIXED_COSTS), Currency.PLN, rates)
        assertEquals(BigDecimal("200"), progress?.planned)
        assertEquals(BigDecimal("50"), progress?.spent)
        assertEquals(0, BigDecimal("25").compareTo(progress?.percent))
    }

    @Test
    fun `budgetProgress clamps percent at 100 even if overpaid`() {
        val items = listOf(item(BudgetSectionType.FIXED_COSTS, "100", paidAmount = "150"))
        val progress = budgetProgress(items, setOf(BudgetSectionType.FIXED_COSTS), Currency.PLN, rates)
        assertEquals(0, BigDecimal("100").compareTo(progress?.percent))
    }

    @Test
    fun `budgetProgress returns null when a rate is missing`() {
        val items = listOf(item(BudgetSectionType.FIXED_COSTS, "100", currency = Currency.GBP))
        assertNull(budgetProgress(items, setOf(BudgetSectionType.FIXED_COSTS), Currency.PLN, rates))
    }

    @Test
    fun `unallocatedAmount is disposable income minus planned spending`() {
        val items = listOf(
            item(BudgetSectionType.INCOME, "5000"),
            item(BudgetSectionType.FIXED_COSTS, "1000"),
            item(BudgetSectionType.SAVINGS, "500")
        )
        assertEquals(BigDecimal("3500"), unallocatedAmount(items, Currency.PLN, rates))
    }

    @Test
    fun `projectedNetWorthDelta adds unpaid income, savings and investments and subtracts unpaid expenses`() {
        val items = listOf(
            item(BudgetSectionType.INCOME, "3000", paidAmount = "1000"),
            item(BudgetSectionType.INCOME_RELATED_EXPENSES, "200", paidAmount = "0"),
            item(BudgetSectionType.SAVINGS, "500", paidAmount = "0"),
            item(BudgetSectionType.INVESTMENTS, "300", paidAmount = "0"),
            item(BudgetSectionType.FIXED_COSTS, "400", paidAmount = "100"),
            item(BudgetSectionType.OTHER_COSTS, "100", paidAmount = "0")
        )
        // remaining: income 2000, expenses 200, savings 500, investments 300, fixed 300, other 100
        // delta = 2000 - 200 + 500 + 300 - 300 - 100 = 2200
        assertEquals(BigDecimal("2200"), projectedNetWorthDelta(items, Currency.PLN, rates))
    }

    @Test
    fun `projectedNetWorthDelta is zero for a budget with no items`() {
        assertEquals(BigDecimal.ZERO, projectedNetWorthDelta(emptyList(), Currency.PLN, rates))
    }
}
