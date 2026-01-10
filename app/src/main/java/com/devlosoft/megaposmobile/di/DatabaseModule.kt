package com.devlosoft.megaposmobile.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.devlosoft.megaposmobile.BuildConfig
import com.devlosoft.megaposmobile.core.common.Constants
import com.devlosoft.megaposmobile.data.local.dao.ActiveTransactionDao
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.database.MIGRATION_1_2
import com.devlosoft.megaposmobile.data.local.database.MIGRATION_2_3
import com.devlosoft.megaposmobile.data.local.database.MIGRATION_3_4
import com.devlosoft.megaposmobile.data.local.database.MIGRATION_4_5
import com.devlosoft.megaposmobile.data.local.database.MIGRATION_6_7
import com.devlosoft.megaposmobile.data.local.database.MegaPosDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): MegaPosDatabase {
        return Room.databaseBuilder(
            context,
            MegaPosDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_6_7)
            .apply {
                // Solo en DEBUG: si falla una migración, borra y recrea la DB
                // En RELEASE: la app crasheará si falta una migración (esto es intencional
                // para detectar errores antes de publicar)
                if (BuildConfig.DEBUG) {
                    fallbackToDestructiveMigration(dropAllTables = true)
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideServerConfigDao(database: MegaPosDatabase): ServerConfigDao {
        return database.serverConfigDao()
    }

    @Provides
    @Singleton
    fun provideActiveTransactionDao(database: MegaPosDatabase): ActiveTransactionDao {
        return database.activeTransactionDao()
    }
}
