package com.walley.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AccountEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class WalleyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
