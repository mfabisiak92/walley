package com.walley.app.data.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSnapshotSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sampleSnapshot = BackupSnapshot(
        schemaVersion = 35,
        exportedAt = "2026-08-03T10:15:30Z",
        accounts = listOf(
            AccountBackupDto(
                id = 1,
                name = "Checking",
                type = "CHECKING",
                currency = "PLN",
                balanceMinorUnits = 123_456,
                taxRate = "STANDARD_19",
                targetAmountMinorUnits = null,
                targetDate = null,
                isDefault = true,
                commissionFlatMinorUnits = 0,
                commissionPercent = "0.19",
                isVirtual = false,
                isClosed = false
            )
        ),
        investments = listOf(
            InvestmentBackupDto(
                id = 1,
                name = "Apple",
                ticker = "AAPL",
                category = "STOCK",
                currency = "USD",
                currentPrice = "231.55",
                accountId = 1,
                lastPriceUpdate = "2026-08-01",
                externalTicker = "AAPL"
            )
        ),
        investmentTransactions = listOf(
            InvestmentTransactionBackupDto(
                id = 1,
                investmentId = 1,
                type = "BUY",
                date = "2026-01-15",
                quantity = "10.5",
                pricePerUnit = "150.25",
                commission = "1.99"
            )
        ),
        assets = listOf(
            AssetBackupDto(
                id = 1,
                name = "Car",
                currency = "PLN",
                purchaseValueMinorUnits = 5_000_000,
                currentValueMinorUnits = 3_500_000,
                purchaseDate = "2023-05-01"
            )
        ),
        liabilities = listOf(
            LiabilityBackupDto(
                id = 1,
                name = "Mortgage",
                currency = "PLN",
                originalAmountMinorUnits = 100_000_000,
                currentBalanceMinorUnits = 80_000_000,
                startDate = "2020-01-01"
            )
        ),
        budgets = listOf(
            BudgetBackupDto(
                id = 1,
                year = 2026,
                month = 8,
                status = "ACTIVE",
                applyIncomeAccountEffects = true,
                applyCostsAccountEffects = true,
                applySavingsAccountEffects = true,
                applyInvestmentsAccountEffects = true
            )
        ),
        budgetItems = listOf(
            BudgetItemBackupDto(
                id = 1,
                budgetId = 1,
                section = "FIXED_COSTS",
                name = "Rent",
                amountMinorUnits = 200_000,
                currency = "PLN",
                accountId = 1,
                paymentDay = 10,
                paymentDayIsLastOfMonth = false,
                paidAmountMinorUnits = 0,
                incomeCategory = null,
                icon = "RENT",
                isFinalized = false
            )
        ),
        adHocBudgets = listOf(
            AdHocBudgetBackupDto(
                id = 1,
                name = "Kitchen renovation",
                startDate = "2026-06-01",
                endDate = "2026-09-01",
                accountId = 1,
                currency = "PLN",
                isCompleted = false,
                isDraft = false
            )
        ),
        adHocBudgetItems = listOf(
            AdHocBudgetItemBackupDto(
                id = 1,
                budgetId = 1,
                name = "Cabinets",
                amountMinorUnits = 500_000,
                paidAmountMinorUnits = 250_000,
                icon = null,
                accountId = null
            )
        ),
        accountOperations = listOf(
            AccountOperationBackupDto(
                id = 1,
                accountId = 1,
                date = "2026-07-01",
                description = "Salary",
                amount = "5000.00"
            )
        ),
        watchedEquities = listOf(WatchedEquityBackupDto(id = 1, name = "Tesla", ticker = "TSLA")),
        equityNotes = listOf(
            EquityNoteBackupDto(id = 1, equityId = 1, date = "2026-07-01", status = "HOLD", note = "Watching earnings")
        ),
        strategyInvestmentLinks = listOf(StrategyInvestmentLinkBackupDto(equityId = 1, investmentId = 1)),
        financialSnapshots = listOf(
            FinancialSnapshotBackupDto(
                id = 1,
                budgetId = 1,
                year = 2026,
                month = 7,
                baseCurrency = "PLN",
                cashAndCheckingMinorUnits = 1_000_000,
                savingsMinorUnits = 2_000_000,
                investmentsMinorUnits = 3_000_000,
                assetsMinorUnits = 4_000_000,
                liabilitiesMinorUnits = 500_000,
                netWorthMinorUnits = 9_500_000,
                incomeMinorUnits = 800_000,
                incomeRelatedExpensesMinorUnits = 50_000,
                disposableIncomeMinorUnits = 750_000,
                salaryIncomeMinorUnits = 700_000,
                dividendsIncomeMinorUnits = 50_000,
                interestIncomeMinorUnits = 30_000,
                otherIncomeMinorUnits = 20_000,
                investmentGrowthMinorUnits = 100_000
            )
        ),
        settings = BackupSettingsDto(
            darkModeOverride = true,
            baseCurrency = "PLN",
            includeSavingsInNetWorth = true,
            fixedCostsTargetPercent = "30",
            otherCostsTargetPercent = null,
            savingsTargetPercent = "20",
            investmentsTargetPercent = null,
            yahooFinanceEnabled = false
        )
    )

    @Test
    fun `snapshot survives an encode-decode round trip`() {
        val encoded = json.encodeToString(BackupSnapshot.serializer(), sampleSnapshot)
        val decoded = json.decodeFromString(BackupSnapshot.serializer(), encoded)

        assertEquals(sampleSnapshot, decoded)
    }

    @Test
    fun `unknown fields in a newer backup format are ignored rather than failing`() {
        val encoded = json.encodeToString(BackupSnapshot.serializer(), sampleSnapshot)
        val withExtraField = encoded.replaceFirst("{", "{\"futureField\":\"ignored\",")

        val decoded = json.decodeFromString(BackupSnapshot.serializer(), withExtraField)

        assertEquals(sampleSnapshot, decoded)
    }
}
