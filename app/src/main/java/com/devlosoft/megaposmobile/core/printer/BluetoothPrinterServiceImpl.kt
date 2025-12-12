package com.devlosoft.megaposmobile.core.printer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Service for Bluetooth printer communication
 */
class BluetoothPrinterServiceImpl(
    private val context: Context,
    private val deviceAddress: String,
    private val printerDriverFactory: PrinterDriverFactory
) : PrinterService {

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
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== testConnection START ==========")
        Log.d(TAG, "Testing Bluetooth connection to $deviceAddress...")

        var socket: BluetoothSocket? = null

        try {
            // Validate device address
            if (deviceAddress.isBlank()) {
                Log.e(TAG, "Bluetooth address is blank")
                return@withContext Result.failure(
                    Exception("El dispositivo Bluetooth no está configurado.\nPor favor, configure la impresora en Opciones Avanzadas.")
                )
            }

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

            // Connect
            Log.d(TAG, "Attempting connection...")
            socket.connect()
            Log.d(TAG, "✓ Connection successful")

            Log.d(TAG, "========== testConnection END (success) ==========")
            Result.success("Impresora Bluetooth conectada correctamente")

        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            Log.d(TAG, "========== testConnection END (error) ==========")
            Result.failure(
                Exception("No se pudo conectar a la impresora Bluetooth.\nVerifique que esté encendida y en rango.")
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            Log.d(TAG, "========== testConnection END (error) ==========")
            Result.failure(
                Exception("Permisos de Bluetooth no concedidos.\nVerifique los permisos de la aplicación.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            Log.d(TAG, "========== testConnection END (error) ==========")
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

    override suspend fun printText(text: String, printerModel: PrinterModel): Result<String> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "========== printText START ==========")
            Log.d(TAG, "Device address: $deviceAddress")
            Log.d(TAG, "Printer Model: ${printerModel.displayName}")
            Log.d(TAG, "Text to print: $text")

            var socket: BluetoothSocket? = null
            var outputStream: OutputStream? = null

            try {
                // Validate device address
                if (deviceAddress.isBlank()) {
                    Log.e(TAG, "Bluetooth address is blank")
                    return@withContext Result.failure(
                        Exception("El dispositivo Bluetooth no está configurado.")
                    )
                }

                // Get the appropriate driver for the printer model
                val driver = printerDriverFactory.createDriver(printerModel)
                Log.d(TAG, "Using driver for: ${driver.getModel().displayName}")

                // Get Bluetooth device
                val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                if (device == null) {
                    Log.e(TAG, "Device not found")
                    return@withContext Result.failure(
                        Exception("Dispositivo Bluetooth no encontrado")
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
}
