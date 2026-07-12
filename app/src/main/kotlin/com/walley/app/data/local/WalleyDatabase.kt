package com.walley.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@Database(
    entities = [
        AccountEntity::class,
        InvestmentEntity::class,
        InvestmentTransactionEntity::class,
        AssetEntity::class,
        BudgetEntity::class,
        BudgetItemEntity::class,
        LiabilityEntity::class,
        FinancialSnapshotEntity::class,
        WatchedEquityEntity::class,
        EquityNoteEntity::class,
        AdHocBudgetEntity::class,
        AdHocBudgetItemEntity::class,
        AccountOperationEntity::class,
        StrategyInvestmentLinkEntity::class
    ],
    version = 31,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WalleyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun assetDao(): AssetDao
    abstract fun budgetDao(): BudgetDao
    abstract fun liabilityDao(): LiabilityDao
    abstract fun financialSnapshotDao(): FinancialSnapshotDao
    abstract fun watchedEquityDao(): WatchedEquityDao
    abstract fun adHocBudgetDao(): AdHocBudgetDao
    abstract fun accountOperationDao(): AccountOperationDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `investments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `ticker` TEXT NOT NULL,
                `quantity` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `price` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN type TEXT NOT NULL DEFAULT 'CHECKING'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE investments ADD COLUMN accountId INTEGER")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN taxRate TEXT NOT NULL DEFAULT 'STANDARD_19'")
        db.execSQL("ALTER TABLE investments ADD COLUMN currentPrice TEXT NOT NULL DEFAULT '0'")
        // Backfill current price with the buy price so existing positions start at zero gain/loss.
        db.execSQL("UPDATE investments SET currentPrice = price")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN targetAmountMinorUnits INTEGER")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `assets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `purchaseValueMinorUnits` INTEGER NOT NULL,
                `currentValueMinorUnits` INTEGER NOT NULL,
                `purchaseDate` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budget_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `budgetId` INTEGER NOT NULL,
                `section` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `amountMinorUnits` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `accountId` INTEGER,
                `paymentDay` INTEGER,
                `paymentDayIsLastOfMonth` INTEGER NOT NULL DEFAULT 0,
                `paidAmountMinorUnits` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budgets ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `liabilities` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `originalAmountMinorUnits` INTEGER NOT NULL,
                `currentBalanceMinorUnits` INTEGER NOT NULL,
                `startDate` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
        // Backfill: the earliest-created existing account becomes the default one.
        db.execSQL("UPDATE accounts SET isDefault = 1 WHERE id = (SELECT MIN(id) FROM accounts)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budget_items ADD COLUMN incomeCategory TEXT")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `financial_snapshots` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `budgetId` INTEGER NOT NULL,
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `baseCurrency` TEXT NOT NULL,
                `cashAndCheckingMinorUnits` INTEGER NOT NULL,
                `savingsMinorUnits` INTEGER NOT NULL,
                `investmentsMinorUnits` INTEGER NOT NULL,
                `assetsMinorUnits` INTEGER NOT NULL,
                `liabilitiesMinorUnits` INTEGER NOT NULL,
                `netWorthMinorUnits` INTEGER NOT NULL,
                `incomeMinorUnits` INTEGER NOT NULL,
                `incomeRelatedExpensesMinorUnits` INTEGER NOT NULL,
                `disposableIncomeMinorUnits` INTEGER NOT NULL,
                `salaryIncomeMinorUnits` INTEGER NOT NULL,
                `dividendsIncomeMinorUnits` INTEGER NOT NULL,
                `interestIncomeMinorUnits` INTEGER NOT NULL,
                `otherIncomeMinorUnits` INTEGER NOT NULL,
                `investmentGrowthMinorUnits` INTEGER
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE budget_items ADD COLUMN icon TEXT")

        // Income items: derive from the existing incomeCategory field.
        db.execSQL("UPDATE budget_items SET icon = 'SALARY' WHERE icon IS NULL AND incomeCategory = 'SALARY'")
        db.execSQL("UPDATE budget_items SET icon = 'DIVIDENDS' WHERE icon IS NULL AND incomeCategory = 'DIVIDENDS'")
        db.execSQL("UPDATE budget_items SET icon = 'INTEREST' WHERE icon IS NULL AND incomeCategory = 'INTEREST'")

        // Savings/Investments items: derive from the section itself.
        db.execSQL("UPDATE budget_items SET icon = 'SAVING' WHERE icon IS NULL AND section = 'SAVINGS'")
        db.execSQL("UPDATE budget_items SET icon = 'INVESTMENT' WHERE icon IS NULL AND section = 'INVESTMENTS'")

        // Income-related expenses / Fixed costs / Other costs: best-effort whole-word keyword match on the name.
        val keywordsByIcon = listOf(
            "RENT" to listOf("rent", "mortgage"),
            "BILLS" to listOf("bill", "bills", "utility", "utilities"),
            "PHONE" to listOf("phone", "mobile"),
            "ELECTRONICS" to listOf("electronics", "electronic", "laptop", "computer"),
            "CURRENCY_EXCHANGE" to listOf("currency", "exchange"),
            "INSURANCE" to listOf("insurance"),
            "GIFT" to listOf("gift", "gifts", "present", "presents"),
            "TRIP" to listOf("trip", "flight", "travel"),
            "VACATIONS" to listOf("vacation", "vacations", "holiday", "holidays"),
            "TRANSPORTATION" to listOf("transport", "transportation", "bus", "train", "metro", "taxi", "uber"),
            "SUBSCRIPTION" to listOf("subscription", "subscriptions", "netflix", "spotify"),
            "ENTERTAINMENT" to listOf("entertainment", "movie", "movies", "cinema"),
            "EATING_OUT" to listOf("restaurant", "restaurants", "dining"),
            "GROCERIES" to listOf("groceries", "grocery", "supermarket"),
            "CLOTHES" to listOf("clothes", "clothing", "apparel"),
            "FUEL" to listOf("fuel", "petrol", "gas"),
            "CAR" to listOf("car", "vehicle"),
            "TAX" to listOf("tax", "taxes")
        )
        keywordsByIcon.forEach { (icon, keywords) ->
            keywords.forEach { keyword ->
                db.execSQL(
                    "UPDATE budget_items SET icon = ? WHERE icon IS NULL AND (' ' || LOWER(name) || ' ') LIKE ?",
                    arrayOf(icon, "% $keyword %")
                )
            }
        }
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watched_equities` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `ticker` TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `equity_notes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `equityId` INTEGER NOT NULL,
                `date` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `note` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Existing investments default to Stock; users can re-categorize them individually.
        db.execSQL("ALTER TABLE investments ADD COLUMN category TEXT NOT NULL DEFAULT 'STOCK'")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The real historical purchase date isn't knowable for existing investments, so default
        // to the day this migration runs; users can correct it afterward.
        db.execSQL("ALTER TABLE investments ADD COLUMN purchaseDate TEXT NOT NULL DEFAULT '${LocalDate.now()}'")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `adhoc_budgets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `startDate` TEXT NOT NULL,
                `endDate` TEXT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `currency` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `adhoc_budget_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `budgetId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `amountMinorUnits` INTEGER NOT NULL,
                `paidAmountMinorUnits` INTEGER NOT NULL DEFAULT 0,
                `icon` TEXT
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Both default to enabled — the existing behavior (paying an item always moves money).
        db.execSQL("ALTER TABLE budgets ADD COLUMN applyAccountEffects INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE adhoc_budgets ADD COLUMN applyAccountEffects INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Splits the single "draw from linked accounts" toggle into one per section group
        // (Income & income-related expenses / Fixed & other costs / Savings / Investments) —
        // ad-hoc budgets don't have these sections, so they keep their single toggle unchanged.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `year` INTEGER NOT NULL,
                `month` INTEGER NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                `applyIncomeAccountEffects` INTEGER NOT NULL DEFAULT 1,
                `applyCostsAccountEffects` INTEGER NOT NULL DEFAULT 1,
                `applySavingsAccountEffects` INTEGER NOT NULL DEFAULT 1,
                `applyInvestmentsAccountEffects` INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO budgets_new (id, year, month, status, applyIncomeAccountEffects, applyCostsAccountEffects, applySavingsAccountEffects, applyInvestmentsAccountEffects)
            SELECT id, year, month, status, applyAccountEffects, applyAccountEffects, applyAccountEffects, applyAccountEffects FROM budgets
            """.trimIndent()
        )
        db.execSQL("DROP TABLE budgets")
        db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ad-hoc budgets always draw from their linked account now — the toggle didn't make
        // sense here (unlike Monthly, there's only one account and no per-section split), so
        // it's removed rather than replaced.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `adhoc_budgets_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `startDate` TEXT NOT NULL,
                `endDate` TEXT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `currency` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO adhoc_budgets_new (id, name, startDate, endDate, accountId, currency)
            SELECT id, name, startDate, endDate, accountId, currency FROM adhoc_budgets
            """.trimIndent()
        )
        db.execSQL("DROP TABLE adhoc_budgets")
        db.execSQL("ALTER TABLE adhoc_budgets_new RENAME TO adhoc_budgets")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Buy/sell events replace the single quantity/price/purchaseDate fields, so multiple
        // purchases of the same ticker accumulate into one position instead of duplicate rows.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `investment_transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `investmentId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `quantity` TEXT NOT NULL,
                `pricePerUnit` TEXT NOT NULL
            )
            """.trimIndent()
        )
        // Seed each existing investment's history with a single BUY transaction from its current fields.
        db.execSQL(
            """
            INSERT INTO investment_transactions (investmentId, type, date, quantity, pricePerUnit)
            SELECT id, 'BUY', purchaseDate, quantity, price FROM investments
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `investments_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `ticker` TEXT NOT NULL,
                `category` TEXT NOT NULL DEFAULT 'STOCK',
                `currency` TEXT NOT NULL,
                `currentPrice` TEXT NOT NULL,
                `accountId` INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO investments_new (id, name, ticker, category, currency, currentPrice, accountId)
            SELECT id, name, ticker, category, currency, currentPrice, accountId FROM investments
            """.trimIndent()
        )
        db.execSQL("DROP TABLE investments")
        db.execSQL("ALTER TABLE investments_new RENAME TO investments")
    }
}

val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Per-account default commission (flat fee or % of trade value, whichever is higher), plus the
        // actual commission charged on each buy/sell event, defaulted from the account's rate but editable.
        db.execSQL("ALTER TABLE accounts ADD COLUMN commissionFlatMinorUnits INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE accounts ADD COLUMN commissionPercent TEXT NOT NULL DEFAULT '0'")
        db.execSQL("ALTER TABLE investment_transactions ADD COLUMN commission TEXT NOT NULL DEFAULT '0'")
    }
}

val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ledger of deposits/withdrawals/transfers/interest applied directly to an investment account's
        // uninvested cash during CSV import — lets a re-imported file recognize an operation it already
        // applied (by account+date+description+amount) instead of double-counting it.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `account_operations` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `accountId` INTEGER NOT NULL,
                `date` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `amount` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ad-hoc budgets previously had no explicit "done" state (the UI derived it from every item
        // being paid off); completion is now an explicit, user-triggered action, same as monthly budgets.
        db.execSQL("ALTER TABLE adhoc_budgets ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Links a watched equity (strategy) to the portfolio investments it applies to.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `strategy_investment_links` (
                `equityId` INTEGER NOT NULL,
                `investmentId` INTEGER NOT NULL,
                PRIMARY KEY(`equityId`, `investmentId`)
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // The auto-synced estimated-tax liability is now labeled "Tax {year}" instead of
        // "Estimated Tax for {year}"; rename existing ones so they keep being recognized by
        // syncEstimatedTaxLiabilities instead of becoming orphaned duplicates.
        db.execSQL(
            """
            UPDATE liabilities
            SET name = 'Tax ' || substr(name, -4)
            WHERE name LIKE 'Estimated Tax for ____'
            """.trimIndent()
        )
    }
}

val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ad-hoc budget items can now each draw from a different account than the budget's default,
        // to support ad-hoc budgets that span multiple currencies.
        db.execSQL("ALTER TABLE adhoc_budget_items ADD COLUMN accountId INTEGER")
    }
}

val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // A virtual account doesn't exist in the real world — its balance is really an earmarked
        // slice of another account's money — so it's excluded from net worth and other totals.
        db.execSQL("ALTER TABLE accounts ADD COLUMN isVirtual INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Ad-hoc budgets now autosave a Draft as the wizard progresses, same as monthly budgets.
        db.execSQL("ALTER TABLE adhoc_budgets ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Soft-close for Saving accounts: kept in the DB (reversible), hidden from pickers used to
        // link something new, and excluded from net worth and other totals.
        db.execSQL("ALTER TABLE accounts ADD COLUMN isClosed INTEGER NOT NULL DEFAULT 0")
    }
}
