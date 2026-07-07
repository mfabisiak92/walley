package com.walley.app.di

import android.content.Context
import androidx.room.Room
import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.AssetDao
import com.walley.app.data.local.BudgetDao
import com.walley.app.data.local.FinancialSnapshotDao
import com.walley.app.data.local.InvestmentDao
import com.walley.app.data.local.LiabilityDao
import com.walley.app.data.local.MIGRATION_1_2
import com.walley.app.data.local.MIGRATION_2_3
import com.walley.app.data.local.MIGRATION_3_4
import com.walley.app.data.local.MIGRATION_4_5
import com.walley.app.data.local.MIGRATION_5_6
import com.walley.app.data.local.MIGRATION_6_7
import com.walley.app.data.local.MIGRATION_7_8
import com.walley.app.data.local.MIGRATION_8_9
import com.walley.app.data.local.MIGRATION_9_10
import com.walley.app.data.local.MIGRATION_10_11
import com.walley.app.data.local.MIGRATION_11_12
import com.walley.app.data.local.MIGRATION_12_13
import com.walley.app.data.local.MIGRATION_13_14
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
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14
            )
            .build()

    @Provides
    fun provideAccountDao(database: WalleyDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideInvestmentDao(database: WalleyDatabase): InvestmentDao = database.investmentDao()

    @Provides
    fun provideAssetDao(database: WalleyDatabase): AssetDao = database.assetDao()

    @Provides
    fun provideBudgetDao(database: WalleyDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideLiabilityDao(database: WalleyDatabase): LiabilityDao = database.liabilityDao()

    @Provides
    fun provideFinancialSnapshotDao(database: WalleyDatabase): FinancialSnapshotDao = database.financialSnapshotDao()
}
