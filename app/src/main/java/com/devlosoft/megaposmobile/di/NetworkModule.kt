package com.devlosoft.megaposmobile.di

import com.devlosoft.megaposmobile.BuildConfig
import com.devlosoft.megaposmobile.core.util.NetworkUtils
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.data.remote.api.AuthApi
import com.devlosoft.megaposmobile.data.remote.api.CashierStationApi
import com.devlosoft.megaposmobile.data.remote.api.CustomerApi
import com.devlosoft.megaposmobile.data.remote.api.FelApi
import com.devlosoft.megaposmobile.data.remote.api.PaymentApi
import com.devlosoft.megaposmobile.data.remote.api.SystemApi
import com.devlosoft.megaposmobile.data.remote.api.TransactionApi
import com.devlosoft.megaposmobile.data.remote.interceptor.AuthInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        sessionManager: SessionManager,
        serverConfigDao: ServerConfigDao,
        networkUtils: NetworkUtils
    ): AuthInterceptor {
        return AuthInterceptor(sessionManager, serverConfigDao, networkUtils)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCashierStationApi(retrofit: Retrofit): CashierStationApi {
        return retrofit.create(CashierStationApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCustomerApi(retrofit: Retrofit): CustomerApi {
        return retrofit.create(CustomerApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTransactionApi(retrofit: Retrofit): TransactionApi {
        return retrofit.create(TransactionApi::class.java)
    }

    @Provides
    @Singleton
    fun providePaymentApi(retrofit: Retrofit): PaymentApi {
        return retrofit.create(PaymentApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSystemApi(retrofit: Retrofit): SystemApi {
        return retrofit.create(SystemApi::class.java)
    }

    @Provides
    @Singleton
    @Named("FelRetrofit")
    fun provideFelRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.FEL_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideFelApi(@Named("FelRetrofit") retrofit: Retrofit): FelApi {
        return retrofit.create(FelApi::class.java)
    }
}
