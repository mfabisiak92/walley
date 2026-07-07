package com.walley.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        InvestmentEntity::class,
        AssetEntity::class,
        BudgetEntity::class,
        BudgetItemEntity::class,
        LiabilityEntity::class,
        FinancialSnapshotEntity::class
    ],
    version = 14,
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
