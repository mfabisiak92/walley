package com.walley.app.domain.model

import java.math.BigDecimal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetItemTest {

    private fun item(amount: String, paidAmount: String) = BudgetItem(
        section = BudgetSectionType.FIXED_COSTS,
        name = "Item",
        amount = BigDecimal(amount),
        currency = Currency.PLN,
        paidAmount = BigDecimal(paidAmount)
    )

    @Test
    fun `isCompleted is true once paid amount reaches the planned amount`() {
        assertTrue(item(amount = "100", paidAmount = "100").isCompleted)
    }

    @Test
    fun `isCompleted is true when overpaid`() {
        assertTrue(item(amount = "100", paidAmount = "150").isCompleted)
    }

    @Test
    fun `isCompleted is false while partially paid`() {
        assertFalse(item(amount = "100", paidAmount = "50").isCompleted)
    }

    @Test
    fun `isCompleted is false for a zero-amount item even if paidAmount is also zero`() {
        assertFalse(item(amount = "0", paidAmount = "0").isCompleted)
    }

    @Test
    fun `hasPaymentDay is true when either a day or last-of-month is set`() {
        val withDay = BudgetItem(
            section = BudgetSectionType.FIXED_COSTS,
            name = "Item",
            amount = BigDecimal.TEN,
            currency = Currency.PLN,
            paymentDay = 15
        )
        val withLastOfMonth = withDay.copy(paymentDay = null, paymentDayIsLastOfMonth = true)
        val withNeither = withDay.copy(paymentDay = null, paymentDayIsLastOfMonth = false)

        assertTrue(withDay.hasPaymentDay)
        assertTrue(withLastOfMonth.hasPaymentDay)
        assertFalse(withNeither.hasPaymentDay)
    }
}
