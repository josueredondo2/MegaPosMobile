package com.devlosoft.megaposmobile.presentation.advancedoptions

import com.devlosoft.megaposmobile.core.util.BluetoothPrinterDevice

sealed class AdvancedOptionsEvent {
    // Field changes
    data class HostnameChanged(val hostname: String) : AdvancedOptionsEvent()
    data class DatafonUrlChanged(val datafonUrl: String) : AdvancedOptionsEvent()
    data class PrinterIpChanged(val printerIp: String) : AdvancedOptionsEvent()

    // Printer mode
    data class PrinterModeChanged(val useIp: Boolean) : AdvancedOptionsEvent()

    // Bluetooth
    data class BluetoothDeviceSelected(val device: BluetoothPrinterDevice) : AdvancedOptionsEvent()
    data object RefreshBluetoothDevices : AdvancedOptionsEvent()
    data object RequestBluetoothPermissions : AdvancedOptionsEvent()
    data class OnPermissionsResult(val granted: Boolean) : AdvancedOptionsEvent()

    // Actions
    data object TestPrinter : AdvancedOptionsEvent()
    data object Save : AdvancedOptionsEvent()
    data object ClearError : AdvancedOptionsEvent()
    data object ClearSavedFlag : AdvancedOptionsEvent()
}
