package com.walley.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetSectionTypeTest {

    @Test
    fun `isSpendingLimit is true only for Fixed and Other costs`() {
        assertTrue(BudgetSectionType.FIXED_COSTS.isSpendingLimit)
        assertTrue(BudgetSectionType.OTHER_COSTS.isSpendingLimit)
        assertFalse(BudgetSectionType.SAVINGS.isSpendingLimit)
        assertFalse(BudgetSectionType.INVESTMENTS.isSpendingLimit)
        assertFalse(BudgetSectionType.INCOME.isSpendingLimit)
    }

    @Test
    fun `isSavingsGoal is true only for Savings and Investments`() {
        assertTrue(BudgetSectionType.SAVINGS.isSavingsGoal)
        assertTrue(BudgetSectionType.INVESTMENTS.isSavingsGoal)
        assertFalse(BudgetSectionType.FIXED_COSTS.isSavingsGoal)
    }

    @Test
    fun `requiresAccount is true for Income, Income-related expenses, Savings, Investments`() {
        assertTrue(BudgetSectionType.INCOME.requiresAccount)
        assertTrue(BudgetSectionType.INCOME_RELATED_EXPENSES.requiresAccount)
        assertTrue(BudgetSectionType.SAVINGS.requiresAccount)
        assertTrue(BudgetSectionType.INVESTMENTS.requiresAccount)
        assertFalse(BudgetSectionType.FIXED_COSTS.requiresAccount)
        assertFalse(BudgetSectionType.OTHER_COSTS.requiresAccount)
    }

    @Test
    fun `isAccountWithdrawal is true for the expense-like sections`() {
        assertTrue(BudgetSectionType.INCOME_RELATED_EXPENSES.isAccountWithdrawal)
        assertTrue(BudgetSectionType.FIXED_COSTS.isAccountWithdrawal)
        assertTrue(BudgetSectionType.OTHER_COSTS.isAccountWithdrawal)
        assertFalse(BudgetSectionType.INCOME.isAccountWithdrawal)
        assertFalse(BudgetSectionType.SAVINGS.isAccountWithdrawal)
        assertFalse(BudgetSectionType.INVESTMENTS.isAccountWithdrawal)
    }

    @Test
    fun `allowedAccountTypes excludes Investment accounts for Fixed and Other costs`() {
        val types = BudgetSectionType.FIXED_COSTS.allowedAccountTypes()
        assertEquals(setOf(AccountType.CHECKING, AccountType.CASH, AccountType.SAVING), types)
    }

    @Test
    fun `allowedAccountTypes is Investment-only for the Investments section`() {
        assertEquals(setOf(AccountType.INVESTMENT), BudgetSectionType.INVESTMENTS.allowedAccountTypes())
    }

    @Test
    fun `allowedAccountTypes is Saving-only for the Savings section`() {
        assertEquals(setOf(AccountType.SAVING), BudgetSectionType.SAVINGS.allowedAccountTypes())
    }

    @Test
    fun `allowedAccountTypes includes Investment accounts for Income sections`() {
        val types = BudgetSectionType.INCOME.allowedAccountTypes()
        assertEquals(setOf(AccountType.CHECKING, AccountType.CASH, AccountType.INVESTMENT), types)
    }
}
