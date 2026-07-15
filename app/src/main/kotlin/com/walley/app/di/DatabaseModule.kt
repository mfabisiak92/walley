package com.walley.app.di

import android.content.Context
import androidx.room.Room
import com.walley.app.data.local.AccountDao
import com.walley.app.data.local.AccountOperationDao
import com.walley.app.data.local.AdHocBudgetDao
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
import com.walley.app.data.local.MIGRATION_14_15
import com.walley.app.data.local.MIGRATION_15_16
import com.walley.app.data.local.MIGRATION_16_17
import com.walley.app.data.local.MIGRATION_17_18
import com.walley.app.data.local.MIGRATION_18_19
import com.walley.app.data.local.MIGRATION_19_20
import com.walley.app.data.local.MIGRATION_20_21
import com.walley.app.data.local.MIGRATION_21_22
import com.walley.app.data.local.MIGRATION_22_23
import com.walley.app.data.local.MIGRATION_23_24
import com.walley.app.data.local.MIGRATION_24_25
import com.walley.app.data.local.MIGRATION_25_26
import com.walley.app.data.local.MIGRATION_26_27
import com.walley.app.data.local.MIGRATION_27_28
import com.walley.app.data.local.MIGRATION_28_29
import com.walley.app.data.local.MIGRATION_29_30
import com.walley.app.data.local.MIGRATION_30_31
import com.walley.app.data.local.MIGRATION_31_32
import com.walley.app.data.local.MIGRATION_32_33
import com.walley.app.data.local.MIGRATION_33_34
import com.walley.app.data.local.MIGRATION_34_35
import com.walley.app.data.local.WalleyDatabase
import com.walley.app.data.local.WatchedEquityDao
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
                MIGRATION_13_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20,
                MIGRATION_20_21,
                MIGRATION_21_22,
                MIGRATION_22_23,
                MIGRATION_23_24,
                MIGRATION_24_25,
                MIGRATION_25_26,
                MIGRATION_26_27,
                MIGRATION_27_28,
                MIGRATION_28_29,
                MIGRATION_29_30,
                MIGRATION_30_31,
                MIGRATION_31_32,
                MIGRATION_32_33,
                MIGRATION_33_34,
                MIGRATION_34_35
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

    @Provides
    fun provideWatchedEquityDao(database: WalleyDatabase): WatchedEquityDao = database.watchedEquityDao()

    @Provides
    fun provideAdHocBudgetDao(database: WalleyDatabase): AdHocBudgetDao = database.adHocBudgetDao()

    @Provides
    fun provideAccountOperationDao(database: WalleyDatabase): AccountOperationDao = database.accountOperationDao()
}
