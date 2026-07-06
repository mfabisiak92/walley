package com.walley.app.di

import com.walley.app.data.repository.AccountRepository
import com.walley.app.data.repository.AccountRepositoryImpl
import com.walley.app.data.repository.ExchangeRateRepository
import com.walley.app.data.repository.ExchangeRateRepositoryImpl
import com.walley.app.data.repository.InvestmentRepository
import com.walley.app.data.repository.InvestmentRepositoryImpl
import com.walley.app.data.repository.SettingsRepository
import com.walley.app.data.repository.SettingsRepositoryImpl
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
}
