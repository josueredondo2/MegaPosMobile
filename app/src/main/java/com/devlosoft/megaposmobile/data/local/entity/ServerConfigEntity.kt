package com.devlosoft.megaposmobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_config")
data class ServerConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val serverUrl: String,
    val serverName: String,
    val isActive: Boolean = true,
    val lastConnected: Long? = null,
    val datafonUrl: String = "",
    val printerIp: String = "",
    val printerBluetoothAddress: String = "",
    val printerBluetoothName: String = "",
    val usePrinterIp: Boolean = true,
    val printerModel: String = "ZEBRA_ZQ511",
    val datafonoProvider: String = "BAC",
    val dataphoneTerminalId: String = ""
)
