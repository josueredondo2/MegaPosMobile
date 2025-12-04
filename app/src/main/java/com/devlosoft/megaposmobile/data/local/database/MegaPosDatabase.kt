package com.devlosoft.megaposmobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.entity.ServerConfigEntity

@Database(
    entities = [ServerConfigEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MegaPosDatabase : RoomDatabase() {
    abstract fun serverConfigDao(): ServerConfigDao
}
