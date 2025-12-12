package com.devlosoft.megaposmobile.core.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isPrinter: Boolean = true
)

@Singleton
class BluetoothPrinterManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    companion object {
        private const val TAG = "BluetoothPrinterMgr"
    }

    /**
     * Check if Bluetooth hardware is available on this device
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Check if Bluetooth is currently enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Check if app has required Bluetooth permissions
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ - Need both BLUETOOTH_SCAN and BLUETOOTH_CONNECT
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 and below
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get list of paired Bluetooth devices
     * @param filterPrinters If true, attempts to filter only printer-like devices
     * @return List of Bluetooth devices
     */
    fun getPairedDevices(filterPrinters: Boolean = false): List<BluetoothPrinterDevice> {
        Log.d(TAG, "========== getPairedDevices START ==========")
        Log.d(TAG, "filterPrinters: $filterPrinters")

        // Check prerequisites
        val btAvailable = isBluetoothAvailable()
        val btEnabled = isBluetoothEnabled()
        val hasPermissions = hasBluetoothPermissions()

        Log.d(TAG, "Bluetooth available: $btAvailable")
        Log.d(TAG, "Bluetooth enabled: $btEnabled")
        Log.d(TAG, "Has permissions: $hasPermissions")

        if (!btAvailable || !btEnabled || !hasPermissions) {
            Log.w(TAG, "Cannot get paired devices - prerequisites not met")
            Log.d(TAG, "========== getPairedDevices END (empty) ==========")
            return emptyList()
        }

        return try {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            Log.d(TAG, "Bonded devices count: ${pairedDevices?.size ?: 0}")

            if (pairedDevices == null || pairedDevices.isEmpty()) {
                Log.w(TAG, "No bonded devices found")
                Log.d(TAG, "========== getPairedDevices END (empty) ==========")
                return emptyList()
            }

            val result = pairedDevices.mapNotNull { device ->
                val deviceName = device.name ?: "Dispositivo Desconocido"
                val deviceAddress = device.address ?: return@mapNotNull null

                Log.d(TAG, "--- Processing device ---")
                Log.d(TAG, "  Name: $deviceName")
                Log.d(TAG, "  Address: $deviceAddress")

                try {
                    val deviceClass = device.bluetoothClass
                    Log.d(TAG, "  Class: ${deviceClass?.deviceClass}")
                    Log.d(TAG, "  Major Class: ${deviceClass?.majorDeviceClass}")
                } catch (e: Exception) {
                    Log.w(TAG, "  Could not get device class: ${e.message}")
                }

                // If filtering, try to identify printers
                val isPrinter = if (filterPrinters) {
                    val result = isPrinterDevice(deviceName, device)
                    Log.d(TAG, "  Is printer (filtered): $result")
                    result
                } else {
                    Log.d(TAG, "  Is printer (not filtering): true")
                    true
                }

                if (!filterPrinters || isPrinter) {
                    Log.d(TAG, "  ✓ INCLUDED in result list")
                    BluetoothPrinterDevice(
                        name = deviceName,
                        address = deviceAddress,
                        isPrinter = isPrinter
                    )
                } else {
                    Log.d(TAG, "  ✗ EXCLUDED from result list")
                    null
                }
            }

            Log.d(TAG, "Total devices returned: ${result.size}")
            result.forEach { device ->
                Log.d(TAG, "  → ${device.name} (${device.address})")
            }
            Log.d(TAG, "========== getPairedDevices END ==========")

            result
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting paired devices: ${e.message}", e)
            Log.d(TAG, "========== getPairedDevices END (exception) ==========")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception getting paired devices: ${e.message}", e)
            Log.d(TAG, "========== getPairedDevices END (exception) ==========")
            emptyList()
        }
    }

    /**
     * Get required permissions based on Android version
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Android 11 and below
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Attempt to identify if a Bluetooth device is likely a printer
     */
    private fun isPrinterDevice(deviceName: String, device: BluetoothDevice): Boolean {
        val printerKeywords = listOf(
            "printer", "impresora", "print", "pos", "receipt",
            "thermal", "star", "epson", "zebra", "bixolon"
        )

        val nameLower = deviceName.lowercase()

        // Check device name for printer keywords
        if (printerKeywords.any { nameLower.contains(it) }) {
            return true
        }

        // Check Bluetooth class (printers typically have class 0x1680 or similar)
        try {
            val deviceClass = device.bluetoothClass
            val majorClass = deviceClass?.majorDeviceClass
            // 0x600 is the major class for imaging devices (printers, scanners)
            if (majorClass == 0x600) {
                return true
            }
        } catch (e: Exception) {
            // Ignore exception
        }

        return false
    }
}
