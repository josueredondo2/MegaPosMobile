package com.devlosoft.megaposmobile.core.printer

import android.util.Log
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Service for network (IP) printer communication
 */
class NetworkPrinterService(
    private val printerIp: String,
    private val printerDriverFactory: PrinterDriverFactory
) : PrinterService {

    companion object {
        private const val TAG = "NetworkPrinterService"
        private const val PRINTER_PORT = 9100
        private const val CONNECTION_TIMEOUT = 5000 // 5 seconds
        private const val TEST_CONNECTION_TIMEOUT = 3000 // 3 seconds for testing
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== testConnection START ==========")
        Log.d(TAG, "Testing IP connection to $printerIp:$PRINTER_PORT...")

        var socket: Socket? = null

        try {
            // Validate IP
            if (printerIp.isBlank()) {
                Log.e(TAG, "Printer IP is blank")
                return@withContext Result.failure(
                    Exception("La IP de la impresora no está configurada.\nPor favor, configure la impresora en Opciones Avanzadas.")
                )
            }

            // Create socket with timeout
            socket = Socket()
            socket.connect(InetSocketAddress(printerIp, PRINTER_PORT), TEST_CONNECTION_TIMEOUT)

            Log.d(TAG, "✓ Connection successful")
            Log.d(TAG, "========== testConnection END (success) ==========")
            Result.success("Impresora conectada correctamente")

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "SocketTimeoutException: ${e.message}", e)
            Log.d(TAG, "========== testConnection END (error) ==========")
            Result.failure(
                Exception("No se pudo conectar a la impresora. Tiempo de espera agotado.\nVerifique que la impresora esté encendida y conectada a la red.")
            )
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "ConnectException: ${e.message}", e)
            Log.d(TAG, "========== testConnection END (error) ==========")
            Result.failure(
                Exception("No se pudo conectar a la impresora en $printerIp.\nVerifique la IP en Opciones Avanzadas.")
            )
        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            Log.d(TAG, "========== testConnection END (error) ==========")
            Result.failure(Exception("Error de conexión con la impresora: ${e.message}"))
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
            Log.d(TAG, "Printer IP: $printerIp")
            Log.d(TAG, "Printer Model: ${printerModel.displayName}")
            Log.d(TAG, "Text to print: $text")

            var socket: Socket? = null
            var outputStream: OutputStream? = null

            try {
                // Validate IP
                if (printerIp.isBlank()) {
                    Log.e(TAG, "Printer IP is blank")
                    return@withContext Result.failure(
                        Exception("La IP de la impresora no está configurada.")
                    )
                }

                // Get the appropriate driver for the printer model
                val driver = printerDriverFactory.createDriver(printerModel)
                Log.d(TAG, "Using driver for: ${driver.getModel().displayName}")

                // Connect to printer via TCP/IP
                Log.d(TAG, "Connecting to printer at $printerIp:$PRINTER_PORT...")
                socket = Socket()
                socket.connect(InetSocketAddress(printerIp, PRINTER_PORT), CONNECTION_TIMEOUT)
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

                Log.d(TAG, "========== printText END (success) ==========")
                Result.success("Impresión exitosa")

            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "SocketTimeoutException: ${e.message}", e)
                Log.d(TAG, "========== printText END (error) ==========")
                Result.failure(
                    Exception("Tiempo de conexión agotado. Verifique la IP y que la impresora esté en la red.")
                )
            } catch (e: java.net.ConnectException) {
                Log.e(TAG, "ConnectException: ${e.message}", e)
                Log.d(TAG, "========== printText END (error) ==========")
                Result.failure(
                    Exception("No se pudo conectar a la impresora. Verifique la IP: $printerIp")
                )
            } catch (e: IOException) {
                Log.e(TAG, "IOException: ${e.message}", e)
                Log.d(TAG, "========== printText END (error) ==========")
                Result.failure(Exception("Error de conexión: ${e.message}"))
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
