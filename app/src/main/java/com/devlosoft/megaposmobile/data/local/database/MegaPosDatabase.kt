package com.devlosoft.megaposmobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.entity.ServerConfigEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add new columns for advanced options
        db.execSQL("ALTER TABLE server_config ADD COLUMN datafonUrl TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE server_config ADD COLUMN printerIp TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE server_config ADD COLUMN printerBluetoothAddress TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE server_config ADD COLUMN printerBluetoothName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE server_config ADD COLUMN usePrinterIp INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add printer model column
        db.execSQL("ALTER TABLE server_config ADD COLUMN printerModel TEXT NOT NULL DEFAULT 'ZEBRA_ZQ511'")
    }
}

@Database(
    entities = [ServerConfigEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MegaPosDatabase : RoomDatabase() {
    abstract fun serverConfigDao(): ServerConfigDao
}
