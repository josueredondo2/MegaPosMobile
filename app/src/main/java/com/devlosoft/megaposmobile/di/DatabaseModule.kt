package com.devlosoft.megaposmobile.di

import android.content.Context
import androidx.room.Room
import com.devlosoft.megaposmobile.core.common.Constants
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.database.MIGRATION_1_2
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
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideServerConfigDao(database: MegaPosDatabase): ServerConfigDao {
        return database.serverConfigDao()
    }
}
