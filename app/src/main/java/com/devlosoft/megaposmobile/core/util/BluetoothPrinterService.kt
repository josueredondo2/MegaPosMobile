package com.devlosoft.megaposmobile.core.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.devlosoft.megaposmobile.core.printer.PrinterDriverFactory
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothPrinterService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val printerDriverFactory: PrinterDriverFactory
) {
    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    companion object {
        private const val TAG = "BluetoothPrinterSvc"
        // UUID estándar para Serial Port Profile (SPP)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        // Puerto estándar para impresoras de red
        private const val PRINTER_PORT = 9100
    }

    /**
     * Sends text to the printer via network IP
     * @param printerIp IP address of the printer
     * @param text Text to print
     * @param printerModel The printer model to use for formatting
     * @return Result with success or error
     */
    suspend fun printTextByIp(
        printerIp: String,
        text: String,
        printerModel: PrinterModel
    ): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== printTextByIp START ==========")
        Log.d(TAG, "Printer IP: $printerIp")
        Log.d(TAG, "Printer Model: ${printerModel.displayName}")
        Log.d(TAG, "Text to print: $text")

        var socket: java.net.Socket? = null
        var outputStream: OutputStream? = null

        try {
            // Get the appropriate driver for the printer model
            val driver = printerDriverFactory.createDriver(printerModel)
            Log.d(TAG, "Using driver for: ${driver.getModel().displayName}")

            // Connect to printer via TCP/IP (port 9100 is standard for printers)
            Log.d(TAG, "Connecting to printer at $printerIp:$PRINTER_PORT...")
            socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(printerIp, PRINTER_PORT), 5000) // 5 seconds timeout
            Log.d(TAG, "✓ Connected successfully")

            // Get output stream
            outputStream = socket.getOutputStream()
            Log.d(TAG, "Output stream obtained")

            // Generate printer commands using the driver
            val printCommands = driver.buildLabel(text)
            Log.d(TAG, "Print commands generated (${printCommands.size} bytes)")

            // Send commands
            Log.d(TAG, "Sending commands to printer...")
            outputStream.write(printCommands)
            outputStream.flush()
            Log.d(TAG, "✓ Commands sent successfully")

            // Small pause to ensure everything was sent
            Thread.sleep(500)

            Log.d(TAG, "========== printTextByIp END (success) ==========")
            Result.success("Impresión exitosa")

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "SocketTimeoutException: ${e.message}", e)
            Log.d(TAG, "========== printTextByIp END (error) ==========")
            Result.failure(Exception("Tiempo de conexión agotado. Verifique la IP y que la impresora esté en la red."))
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "ConnectException: ${e.message}", e)
            Log.d(TAG, "========== printTextByIp END (error) ==========")
            Result.failure(Exception("No se pudo conectar a la impresora. Verifique la IP: $printerIp"))
        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            Log.d(TAG, "========== printTextByIp END (error) ==========")
            Result.failure(Exception("Error de conexión: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            Log.d(TAG, "========== printTextByIp END (error) ==========")
            Result.failure(Exception("Error inesperado: ${e.message}"))
        } finally {
            // Close resources
            try {
                outputStream?.close()
                socket?.close()
                Log.d(TAG, "Resources closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }

    /**
     * Sends text to the Bluetooth printer
     * @param deviceAddress MAC address of the Bluetooth device
     * @param text Text to print
     * @param printerModel The printer model to use for formatting
     * @return Result with success or error
     */
    suspend fun printText(
        deviceAddress: String,
        text: String,
        printerModel: PrinterModel
    ): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== printText START ==========")
        Log.d(TAG, "Device address: $deviceAddress")
        Log.d(TAG, "Printer Model: ${printerModel.displayName}")
        Log.d(TAG, "Text to print: $text")

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            // Get the appropriate driver for the printer model
            val driver = printerDriverFactory.createDriver(printerModel)
            Log.d(TAG, "Using driver for: ${driver.getModel().displayName}")

            // Get Bluetooth device
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (device == null) {
                Log.e(TAG, "Device not found")
                return@withContext Result.failure(Exception("Dispositivo Bluetooth no encontrado"))
            }

            Log.d(TAG, "Device found: ${device.name}")

            // Create socket
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            Log.d(TAG, "Socket created")

            // Cancel discovery to improve connection
            try {
                bluetoothAdapter?.cancelDiscovery()
                Log.d(TAG, "Discovery cancelled")
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not cancel discovery (permission issue): ${e.message}")
            }

            // Connect
            Log.d(TAG, "Connecting to device...")
            socket.connect()
            Log.d(TAG, "✓ Connected successfully")

            // Get output stream
            outputStream = socket.outputStream
            Log.d(TAG, "Output stream obtained")

            // Generate printer commands using the driver
            val printCommands = driver.buildLabel(text)
            Log.d(TAG, "Print commands generated (${printCommands.size} bytes)")

            // Send commands
            Log.d(TAG, "Sending commands to printer...")
            outputStream.write(printCommands)
            outputStream.flush()
            Log.d(TAG, "✓ Commands sent successfully")

            // Small pause to ensure everything was sent
            Thread.sleep(500)

            Log.d(TAG, "========== printText END (success) ==========")
            Result.success("Impresión exitosa")

        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            Log.d(TAG, "========== printText END (error) ==========")
            Result.failure(Exception("Error de conexión: ${e.message}"))
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            Log.d(TAG, "========== printText END (error) ==========")
            Result.failure(Exception("Permisos de Bluetooth no concedidos"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            Log.d(TAG, "========== printText END (error) ==========")
            Result.failure(Exception("Error inesperado: ${e.message}"))
        } finally {
            // Close resources
            try {
                outputStream?.close()
                socket?.close()
                Log.d(TAG, "Resources closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing resources: ${e.message}")
            }
        }
    }

    // Legacy methods for backwards compatibility - use default Zebra ZQ511
    @Deprecated("Use printTextByIp with printerModel parameter", ReplaceWith("printTextByIp(printerIp, text, PrinterModel.ZEBRA_ZQ511)"))
    suspend fun printTestTextByIp(printerIp: String, text: String): Result<String> {
        return printTextByIp(printerIp, text, PrinterModel.ZEBRA_ZQ511)
    }

    @Deprecated("Use printText with printerModel parameter", ReplaceWith("printText(deviceAddress, text, PrinterModel.ZEBRA_ZQ511)"))
    suspend fun printTestText(deviceAddress: String, text: String): Result<String> {
        return printText(deviceAddress, text, PrinterModel.ZEBRA_ZQ511)
    }

    /**
     * Tests printer connection to verify it's active and responding
     * @param printerIp IP address of the printer (used if usePrinterIp is true)
     * @param printerBluetoothAddress MAC address of Bluetooth device (used if usePrinterIp is false)
     * @param usePrinterIp true for IP connection, false for Bluetooth
     * @param printerModel The printer model to use
     * @return Result with success or error message
     */
    suspend fun testPrinterConnection(
        printerIp: String,
        printerBluetoothAddress: String,
        usePrinterIp: Boolean,
        printerModel: PrinterModel
    ): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== testPrinterConnection START ==========")
        Log.d(TAG, "Mode: ${if (usePrinterIp) "IP" else "Bluetooth"}")

        // Validate configuration
        if (usePrinterIp) {
            if (printerIp.isBlank()) {
                Log.e(TAG, "Printer IP is blank")
                Log.d(TAG, "========== testPrinterConnection END (error) ==========")
                return@withContext Result.failure(
                    Exception("La IP de la impresora no está configurada. Por favor, configure la impresora en Opciones Avanzadas.")
                )
            }
        } else {
            if (printerBluetoothAddress.isBlank()) {
                Log.e(TAG, "Bluetooth address is blank")
                Log.d(TAG, "========== testPrinterConnection END (error) ==========")
                return@withContext Result.failure(
                    Exception("El dispositivo Bluetooth no está configurado. Por favor, configure la impresora en Opciones Avanzadas.")
                )
            }
        }

        // Test connection based on mode
        return@withContext if (usePrinterIp) {
            testIpConnection(printerIp, printerModel)
        } else {
            testBluetoothConnection(printerBluetoothAddress, printerModel)
        }
    }

    /**
     * Tests IP printer connection
     */
    private suspend fun testIpConnection(
        printerIp: String,
        printerModel: PrinterModel
    ): Result<String> = withContext(Dispatchers.IO) {
        var socket: java.net.Socket? = null

        try {
            Log.d(TAG, "Testing IP connection to $printerIp:$PRINTER_PORT...")

            // Create socket with timeout
            socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(printerIp, PRINTER_PORT), 3000) // 3 seconds timeout

            Log.d(TAG, "✓ Connection successful")
            Log.d(TAG, "========== testPrinterConnection END (success) ==========")
            Result.success("Impresora conectada correctamente")

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "SocketTimeoutException: ${e.message}", e)
            Log.d(TAG, "========== testPrinterConnection END (error) ==========")
            Result.failure(Exception("No se pudo conectar a la impresora. Tiempo de espera agotado.\nVerifique que la impresora esté encendida y conectada a la red."))
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "ConnectException: ${e.message}", e)
            Log.d(TAG, "========== testPrinterConnection END (error) ==========")
            Result.failure(Exception("No se pudo conectar a la impresora en $printerIp.\nVerifique la IP en Opciones Avanzadas."))
        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            Log.d(TAG, "========== testPrinterConnection END (error) ==========")
            Result.failure(Exception("Error de conexión con la impresora: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            Log.d(TAG, "========== testPrinterConnection END (error) ==========")
            Result.failure(Exception("Error inesperado al conectar con la impresora: ${e.message}"))
        } finally {
            try {
                socket?.close()
                Log.d(TAG, "Test socket closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing test socket: ${e.message}")
            }
        }
    }

    /**
     * Tests Bluetooth printer connection
     */
    private suspend fun testBluetoothConnection(
        deviceAddress: String,
        printerModel: PrinterModel
    ): Result<String> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null

        try {
            Log.d(TAG, "Testing Bluetooth connection to $deviceAddress...")

            // Get Bluetooth device
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (device == null) {
                Log.e(TAG, "Bluetooth device not found")
                return@withContext Result.failure(
                    Exception("Dispositivo Bluetooth no encontrado.\nVerifique que el dispositivo esté emparejado.")
                )
            }

            Log.d(TAG, "Device found: ${device.name ?: "Unknown"}")

            // Create socket
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            Log.d(TAG, "Socket created")

            // Cancel discovery to improve connection
            try {
                bluetoothAdapter?.cancelDiscovery()
                Log.d(TAG, "Discovery cancelled")
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not cancel discovery (permission issue): ${e.message}")
            }

            // Connect with timeout (we'll rely on the default timeout)
            Log.d(TAG, "Attempting connection...")
            socket.connect()
            Log.d(TAG, "✓ Connection successful")

            Log.d(TAG, "========== testPrinterConnection END (success) ==========")
            Result.success("Impresora Bluetooth conectada correctamente")

        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            Log.d(TAG, "========== testPrinterConnection END (error) ==========")
            Result.failure(Exception("No se pudo conectar a la impresora Bluetooth.\nVerifique que esté encendida y en rango."))
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            Log.d(TAG, "========== testPrinterConnection END (error) ==========")
            Result.failure(Exception("Permisos de Bluetooth no concedidos.\nVerifique los permisos de la aplicación."))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            Log.d(TAG, "========== testPrinterConnection END (error) ==========")
            Result.failure(Exception("Error inesperado al conectar con la impresora: ${e.message}"))
        } finally {
            try {
                socket?.close()
                Log.d(TAG, "Test socket closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing test socket: ${e.message}")
            }
        }
    }
}
