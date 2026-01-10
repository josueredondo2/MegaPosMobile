package com.devlosoft.megaposmobile.di

import com.devlosoft.megaposmobile.data.repository.AuditRepositoryImpl
import com.devlosoft.megaposmobile.data.repository.AuthRepositoryImpl
import com.devlosoft.megaposmobile.data.repository.BillingRepositoryImpl
import com.devlosoft.megaposmobile.data.repository.CashierStationRepositoryImpl
import com.devlosoft.megaposmobile.data.repository.PaymentRepositoryImpl
import com.devlosoft.megaposmobile.data.repository.SystemRepositoryImpl
import com.devlosoft.megaposmobile.data.repository.CatalogRepositoryImpl
import com.devlosoft.megaposmobile.domain.repository.AuditRepository
import com.devlosoft.megaposmobile.domain.repository.AuthRepository
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import com.devlosoft.megaposmobile.domain.repository.CashierStationRepository
import com.devlosoft.megaposmobile.domain.repository.PaymentRepository
import com.devlosoft.megaposmobile.domain.repository.SystemRepository
import com.devlosoft.megaposmobile.domain.repository.CatalogRepository
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

    @Binds
    @Singleton
    abstract fun bindBillingRepository(
        billingRepositoryImpl: BillingRepositoryImpl
    ): BillingRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        paymentRepositoryImpl: PaymentRepositoryImpl
    ): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindAuditRepository(
        auditRepositoryImpl: AuditRepositoryImpl
    ): AuditRepository

    @Binds
    @Singleton
    abstract fun bindSystemRepository(
        systemRepositoryImpl: SystemRepositoryImpl
    ): SystemRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(
        catalogRepositoryImpl: CatalogRepositoryImpl
    ): CatalogRepository
}
