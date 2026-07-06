package com.walley.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AccountEntity::class, InvestmentEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WalleyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun investmentDao(): InvestmentDao
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
