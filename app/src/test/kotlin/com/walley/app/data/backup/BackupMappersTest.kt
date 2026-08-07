package com.walley.app.data.backup

import com.walley.app.data.local.AccountEntity
import com.walley.app.data.local.AccountOperationEntity
import com.walley.app.data.local.BudgetEntity
import com.walley.app.data.local.BudgetItemEntity
import com.walley.app.data.local.InvestmentTransactionEntity
import com.walley.app.domain.model.AccountTaxRate
import com.walley.app.domain.model.AccountType
import com.walley.app.domain.model.BudgetItemIcon
import com.walley.app.domain.model.BudgetSectionType
import com.walley.app.domain.model.BudgetStatus
import com.walley.app.domain.model.Currency
import com.walley.app.domain.model.IncomeCategory
import com.walley.app.domain.model.InvestmentTransactionType
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMappersTest {

    @Test
    fun `account entity survives a dto round trip, including BigDecimal and LocalDate fields`() {
        val original = AccountEntity(
            id = 42,
            name = "Brokerage",
            type = AccountType.INVESTMENT,
            currency = Currency.USD,
            balanceMinorUnits = 10_000_00,
            taxRate = AccountTaxRate.STANDARD_19,
            targetAmountMinorUnits = 50_000_00,
            targetDate = LocalDate.of(2027, 1, 1),
            isDefault = false,
            commissionFlatMinorUnits = 500,
            commissionPercent = BigDecimal("0.39"),
            isVirtual = true,
            isClosed = false
        )

        val restored = original.toBackupDto().toEntity(newId = original.id)

        assertEquals(original, restored)
    }

    @Test
    fun `account entity with null optional fields round trips`() {
        val original = AccountEntity(
            id = 1,
            name = "Cash",
            type = AccountType.CASH,
            currency = Currency.PLN,
            balanceMinorUnits = 0,
            targetAmountMinorUnits = null,
            targetDate = null
        )

        val restored = original.toBackupDto().toEntity(newId = original.id)

        assertEquals(original, restored)
    }

    @Test
    fun `investment transaction preserves BigDecimal precision through the dto`() {
        val original = InvestmentTransactionEntity(
            id = 7,
            investmentId = 3,
            type = InvestmentTransactionType.SELL,
            date = LocalDate.of(2026, 2, 14),
            quantity = BigDecimal("12.345678"),
            pricePerUnit = BigDecimal("99.990000"),
            commission = BigDecimal.ZERO
        )

        val dto = original.toBackupDto()
        val restored = dto.toEntity(remappedInvestmentId = original.investmentId)

        assertEquals(original.copy(id = 0), restored)
    }

    @Test
    fun `budget item preserves nullable icon and income category through the dto`() {
        val withOptionalFields = BudgetItemEntity(
            id = 5,
            budgetId = 10,
            section = BudgetSectionType.INCOME,
            name = "Salary",
            amountMinorUnits = 800_000,
            currency = Currency.PLN,
            accountId = 2,
            incomeCategory = IncomeCategory.SALARY,
            icon = BudgetItemIcon.SALARY
        )

        val restored = withOptionalFields.toBackupDto().toEntity(remappedBudgetId = 10, remappedAccountId = 2)

        assertEquals(withOptionalFields.copy(id = 0), restored)
    }

    @Test
    fun `budget entity preserves a set planned net worth through the dto`() {
        val original = BudgetEntity(
            id = 8,
            year = 2026,
            month = 3,
            status = BudgetStatus.ACTIVE,
            applyIncomeAccountEffects = true,
            applyCostsAccountEffects = false,
            applySavingsAccountEffects = true,
            applyInvestmentsAccountEffects = false,
            plannedNetWorthMinorUnits = 150_000_00
        )

        val restored = original.toBackupDto().toEntity(newId = original.id)

        assertEquals(original, restored)
    }

    @Test
    fun `budget entity predating the planned net worth field round trips it as null`() {
        val original = BudgetEntity(
            id = 9,
            year = 2026,
            month = 4,
            status = BudgetStatus.DRAFT,
            plannedNetWorthMinorUnits = null
        )

        val restored = original.toBackupDto().toEntity(newId = original.id)

        assertEquals(original, restored)
    }

    @Test
    fun `account operation preserves amount sign through the dto`() {
        val original = AccountOperationEntity(
            id = 9,
            accountId = 4,
            date = LocalDate.of(2026, 3, 3),
            description = "Withdrawal",
            amount = BigDecimal("-250.00")
        )

        val restored = original.toBackupDto().toEntity(remappedAccountId = original.accountId)

        assertEquals(original.copy(id = 0), restored)
    }
}
