package com.walley.app.data.backup

import kotlinx.serialization.Serializable

/**
 * Wire format for a full data export/restore. Money and dates are stored as plain strings
 * (matching the exact conversions [com.walley.app.data.local.Converters] uses for Room) rather
 * than relying on kotlinx.serialization's numeric/date handling, so the format is stable and
 * legible regardless of serialization library defaults.
 */
@Serializable
data class BackupSnapshot(
    val schemaVersion: Int,
    val exportedAt: String,
    val accounts: List<AccountBackupDto>,
    val investments: List<InvestmentBackupDto>,
    val investmentTransactions: List<InvestmentTransactionBackupDto>,
    val assets: List<AssetBackupDto>,
    val liabilities: List<LiabilityBackupDto>,
    val budgets: List<BudgetBackupDto>,
    val budgetItems: List<BudgetItemBackupDto>,
    val adHocBudgets: List<AdHocBudgetBackupDto>,
    val adHocBudgetItems: List<AdHocBudgetItemBackupDto>,
    val accountOperations: List<AccountOperationBackupDto>,
    val watchedEquities: List<WatchedEquityBackupDto>,
    val equityNotes: List<EquityNoteBackupDto>,
    val strategyInvestmentLinks: List<StrategyInvestmentLinkBackupDto>,
    val financialSnapshots: List<FinancialSnapshotBackupDto>,
    val settings: BackupSettingsDto
)

@Serializable
data class AccountBackupDto(
    val id: Long,
    val name: String,
    val type: String,
    val currency: String,
    val balanceMinorUnits: Long,
    val taxRate: String,
    val targetAmountMinorUnits: Long?,
    val targetDate: String?,
    val isDefault: Boolean,
    val commissionFlatMinorUnits: Long,
    val commissionPercent: String,
    val isVirtual: Boolean,
    val isClosed: Boolean
)

@Serializable
data class InvestmentBackupDto(
    val id: Long,
    val name: String,
    val ticker: String,
    val category: String,
    val currency: String,
    val currentPrice: String,
    val accountId: Long?,
    val lastPriceUpdate: String?,
    val externalTicker: String?,
    // Defaulted to null so a backup taken before this field existed still restores cleanly.
    val previousPrice: String? = null
)

@Serializable
data class InvestmentTransactionBackupDto(
    val id: Long,
    val investmentId: Long,
    val type: String,
    val date: String,
    val quantity: String,
    val pricePerUnit: String,
    val commission: String
)

@Serializable
data class AssetBackupDto(
    val id: Long,
    val name: String,
    val currency: String,
    val purchaseValueMinorUnits: Long,
    val currentValueMinorUnits: Long,
    val purchaseDate: String
)

@Serializable
data class LiabilityBackupDto(
    val id: Long,
    val name: String,
    val currency: String,
    val originalAmountMinorUnits: Long,
    val currentBalanceMinorUnits: Long,
    val startDate: String
)

@Serializable
data class BudgetBackupDto(
    val id: Long,
    val year: Int,
    val month: Int,
    val status: String,
    val applyIncomeAccountEffects: Boolean,
    val applyCostsAccountEffects: Boolean,
    val applySavingsAccountEffects: Boolean,
    val applyInvestmentsAccountEffects: Boolean,
    // Defaults to null so a backup taken before this field existed still restores.
    val plannedNetWorthMinorUnits: Long? = null
)

@Serializable
data class BudgetItemBackupDto(
    val id: Long,
    val budgetId: Long,
    val section: String,
    val name: String,
    val amountMinorUnits: Long,
    val currency: String,
    val accountId: Long?,
    val paymentDay: Int?,
    val paymentDayIsLastOfMonth: Boolean,
    val paidAmountMinorUnits: Long,
    val incomeCategory: String?,
    val icon: String?,
    val isFinalized: Boolean
)

@Serializable
data class AdHocBudgetBackupDto(
    val id: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val accountId: Long,
    val currency: String,
    val isCompleted: Boolean,
    val isDraft: Boolean
)

@Serializable
data class AdHocBudgetItemBackupDto(
    val id: Long,
    val budgetId: Long,
    val name: String,
    val amountMinorUnits: Long,
    val paidAmountMinorUnits: Long,
    val icon: String?,
    val accountId: Long?
)

@Serializable
data class AccountOperationBackupDto(
    val id: Long,
    val accountId: Long,
    val date: String,
    val description: String,
    val amount: String
)

@Serializable
data class WatchedEquityBackupDto(
    val id: Long,
    val name: String,
    val ticker: String?
)

@Serializable
data class EquityNoteBackupDto(
    val id: Long,
    val equityId: Long,
    val date: String,
    val status: String,
    val note: String
)

@Serializable
data class StrategyInvestmentLinkBackupDto(
    val equityId: Long,
    val investmentId: Long
)

@Serializable
data class FinancialSnapshotBackupDto(
    val id: Long,
    val budgetId: Long,
    val year: Int,
    val month: Int,
    val baseCurrency: String,
    val cashAndCheckingMinorUnits: Long,
    val savingsMinorUnits: Long,
    val investmentsMinorUnits: Long,
    val assetsMinorUnits: Long,
    val liabilitiesMinorUnits: Long,
    val netWorthMinorUnits: Long,
    val incomeMinorUnits: Long,
    val incomeRelatedExpensesMinorUnits: Long,
    val disposableIncomeMinorUnits: Long,
    val salaryIncomeMinorUnits: Long,
    val dividendsIncomeMinorUnits: Long,
    val interestIncomeMinorUnits: Long,
    val otherIncomeMinorUnits: Long,
    val investmentGrowthMinorUnits: Long?
)

@Serializable
data class BackupSettingsDto(
    val darkModeOverride: Boolean?,
    val baseCurrency: String,
    val includeSavingsInNetWorth: Boolean,
    val fixedCostsTargetPercent: String?,
    val otherCostsTargetPercent: String?,
    val savingsTargetPercent: String?,
    val investmentsTargetPercent: String?,
    val yahooFinanceEnabled: Boolean
)
