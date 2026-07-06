package com.walley.app.di

import android.content.Context
import androidx.room.Room
import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.MIGRATION_1_2
import com.walley.app.data.local.MIGRATION_2_3
import com.walley.app.data.local.WalleyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WalleyDatabase =
        Room.databaseBuilder(context, WalleyDatabase::class.java, "walley.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideAccountDao(database: WalleyDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideInvestmentDao(database: WalleyDatabase): InvestmentDao = database.investmentDao()
}
