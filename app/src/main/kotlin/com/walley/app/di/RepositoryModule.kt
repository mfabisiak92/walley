package com.walley.app.di

import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AccountRepositoryImpl
import com.walley.app.data.repository.AssetRepository
import com.walley.app.data.repository.AssetRepositoryImpl
import com.walley.app.data.repository.BudgetRepository
import com.walley.app.data.repository.BudgetRepositoryImpl
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.ExchangeRateRepositoryImpl
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.InvestmentRepositoryImpl
import com.walley.app.data.repository.LiabilityRepository
import com.walley.app.data.repository.LiabilityRepositoryImpl
import com.walley.app.data.repository.SecurityRepository
import com.walley.app.data.repository.SecurityRepositoryImpl
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.data.repository.SettingsRepositoryImpl
import com.walley.app.data.repository.SnapshotRepository
import com.walley.app.data.repository.SnapshotRepositoryImpl
import com.walley.app.data.repository.WatchedEquityRepository
import com.walley.app.data.repository.WatchedEquityRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindInvestmentRepository(impl: InvestmentRepositoryImpl): InvestmentRepository

    @Binds
    abstract fun bindExchangeRateRepository(impl: ExchangeRateRepositoryImpl): ExchangeRateRepository

    @Binds
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository

    @Binds
    abstract fun bindAssetRepository(impl: AssetRepositoryImpl): AssetRepository

    @Binds
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    abstract fun bindLiabilityRepository(impl: LiabilityRepositoryImpl): LiabilityRepository

    @Binds
    abstract fun bindSnapshotRepository(impl: SnapshotRepositoryImpl): SnapshotRepository

    @Binds
    abstract fun bindWatchedEquityRepository(impl: WatchedEquityRepositoryImpl): WatchedEquityRepository
}
