package com.devlosoft.megaposmobile.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.devlosoft.megaposmobile.data.local.dao.ActiveTransactionDao
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.entity.ActiveTransactionEntity
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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create active_transaction table to persist current transaction ID
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS active_transaction (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                transactionId TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add dataphone terminal ID column to persist the terminal ID from PAX payments
        db.execSQL("ALTER TABLE server_config ADD COLUMN dataphoneTerminalId TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [ServerConfigEntity::class, ActiveTransactionEntity::class],
    version = 5,
    exportSchema = false
)
abstract class MegaPosDatabase : RoomDatabase() {
    abstract fun serverConfigDao(): ServerConfigDao
    abstract fun activeTransactionDao(): ActiveTransactionDao
}
