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
        BudgetItemEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WalleyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun assetDao(): AssetDao
    abstract fun budgetDao(): BudgetDao
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
