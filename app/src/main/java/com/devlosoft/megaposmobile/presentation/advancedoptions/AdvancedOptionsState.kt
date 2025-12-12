package com.devlosoft.megaposmobile.presentation.advancedoptions

import com.devlosoft.megaposmobile.core.util.BluetoothPrinterDevice
import com.devlosoft.megaposmobile.domain.model.PrinterModel

data class AdvancedOptionsState(
    // Configuration fields
    val hostname: String = "",
    val datafonUrl: String = "",
    val printerIp: String = "",

    // Printer model
    val printerModel: PrinterModel = PrinterModel.ZEBRA_ZQ511,

    // Bluetooth printer
    val selectedBluetoothDevice: BluetoothPrinterDevice? = null,
    val bluetoothDevices: List<BluetoothPrinterDevice> = emptyList(),

    // Bluetooth status
    val isBluetoothAvailable: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothPermissions: Boolean = false,

    // Printer mode (true = IP, false = Bluetooth)
    val usePrinterIp: Boolean = true,

    // UI states
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isTestingPrinter: Boolean = false,
    val error: String? = null
)
