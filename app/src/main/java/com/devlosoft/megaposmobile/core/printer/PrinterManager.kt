package com.devlosoft.megaposmobile.core.printer

import android.content.Context
import android.util.Log
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager that coordinates printer services and determines which one to use
 * based on the configuration (Bluetooth or Network IP)
 */
@Singleton
class PrinterManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverConfigDao: ServerConfigDao,
    private val printerDriverFactory: PrinterDriverFactory
) {
    companion object {
        private const val TAG = "PrinterManager"
    }

    /**
     * Tests the printer connection based on the current configuration
     * @return Result with success or error message
     */
    suspend fun testPrinterConnection(): Result<String> {
        Log.d(TAG, "Testing printer connection...")

        val config = serverConfigDao.getActiveServerConfigSync()
        if (config == null) {
            Log.e(TAG, "No server configuration found")
            return Result.failure(
                Exception("No hay configuración de impresora.\nPor favor, configure la impresora en Opciones Avanzadas.")
            )
        }

        val printerService = createPrinterService(
            usePrinterIp = config.usePrinterIp,
            printerIp = config.printerIp,
            bluetoothAddress = config.printerBluetoothAddress
        )

        return printerService.testConnection()
    }

    /**
     * Prints text using the configured printer
     * @param text Text to print
     * @return Result with success or error message
     */
    suspend fun printText(text: String): Result<String> {
        Log.d(TAG, "Printing text...")

        val config = serverConfigDao.getActiveServerConfigSync()
        if (config == null) {
            Log.e(TAG, "No server configuration found")
            return Result.failure(
                Exception("No hay configuración de impresora.")
            )
        }

        val printerModel = PrinterModel.fromString(config.printerModel)
        val printerService = createPrinterService(
            usePrinterIp = config.usePrinterIp,
            printerIp = config.printerIp,
            bluetoothAddress = config.printerBluetoothAddress
        )

        return printerService.printText(text, printerModel)
    }

    /**
     * Prints text using specific configuration (for testing purposes)
     * @param text Text to print
     * @param printerIp IP address (if using network)
     * @param bluetoothAddress Bluetooth address (if using Bluetooth)
     * @param usePrinterIp true for network, false for Bluetooth
     * @param printerModel The printer model
     * @return Result with success or error message
     */
    suspend fun printTextWithConfig(
        text: String,
        printerIp: String,
        bluetoothAddress: String,
        usePrinterIp: Boolean,
        printerModel: PrinterModel
    ): Result<String> {
        Log.d(TAG, "Printing text with custom config...")

        val printerService = createPrinterService(
            usePrinterIp = usePrinterIp,
            printerIp = printerIp,
            bluetoothAddress = bluetoothAddress
        )

        return printerService.printText(text, printerModel)
    }

    /**
     * Creates the appropriate printer service based on configuration
     */
    private fun createPrinterService(
        usePrinterIp: Boolean,
        printerIp: String,
        bluetoothAddress: String
    ): PrinterService {
        return if (usePrinterIp) {
            Log.d(TAG, "Using NetworkPrinterService with IP: $printerIp")
            NetworkPrinterService(
                printerIp = printerIp,
                printerDriverFactory = printerDriverFactory
            )
        } else {
            Log.d(TAG, "Using BluetoothPrinterService with address: $bluetoothAddress")
            BluetoothPrinterServiceImpl(
                context = context,
                deviceAddress = bluetoothAddress,
                printerDriverFactory = printerDriverFactory
            )
        }
    }
}
