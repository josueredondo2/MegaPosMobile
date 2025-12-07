package com.devlosoft.megaposmobile.di

import com.devlosoft.megaposmobile.data.repository.AuthRepositoryImpl
import com.devlosoft.megaposmobile.data.repository.CashierStationRepositoryImpl
import com.devlosoft.megaposmobile.domain.repository.AuthRepository
import com.devlosoft.megaposmobile.domain.repository.CashierStationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCashierStationRepository(
        cashierStationRepositoryImpl: CashierStationRepositoryImpl
    ): CashierStationRepository
}
