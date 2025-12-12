package com.devlosoft.megaposmobile.core.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
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
    @ApplicationContext private val context: Context
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
     * Envía un texto de prueba a la impresora por red IP
     * @param printerIp Dirección IP de la impresora
     * @param text Texto a imprimir
     * @return Result con éxito o error
     */
    suspend fun printTestTextByIp(printerIp: String, text: String): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== printTestTextByIp START ==========")
        Log.d(TAG, "Printer IP: $printerIp")
        Log.d(TAG, "Text to print: $text")

        var socket: java.net.Socket? = null
        var outputStream: OutputStream? = null

        try {
            // Conectar a la impresora por TCP/IP (puerto 9100 es estándar para impresoras)
            Log.d(TAG, "Connecting to printer at $printerIp:$PRINTER_PORT...")
            socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(printerIp, PRINTER_PORT), 5000) // 5 segundos timeout
            Log.d(TAG, "✓ Connected successfully")

            // Obtener output stream
            outputStream = socket.getOutputStream()
            Log.d(TAG, "Output stream obtained")

            // Generar comandos ZPL
            val zplCommands = buildZPLTestLabel(text)
            Log.d(TAG, "ZPL commands generated:")
            Log.d(TAG, zplCommands)

            // Enviar comandos
            Log.d(TAG, "Sending commands to printer...")
            outputStream.write(zplCommands.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            Log.d(TAG, "✓ Commands sent successfully")

            // Pequeña pausa para asegurar que se envió todo
            Thread.sleep(500)

            Log.d(TAG, "========== printTestTextByIp END (success) ==========")
            Result.success("Impresión exitosa")

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "SocketTimeoutException: ${e.message}", e)
            Log.d(TAG, "========== printTestTextByIp END (error) ==========")
            Result.failure(Exception("Tiempo de conexión agotado. Verifique la IP y que la impresora esté en la red."))
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "ConnectException: ${e.message}", e)
            Log.d(TAG, "========== printTestTextByIp END (error) ==========")
            Result.failure(Exception("No se pudo conectar a la impresora. Verifique la IP: $printerIp"))
        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            Log.d(TAG, "========== printTestTextByIp END (error) ==========")
            Result.failure(Exception("Error de conexión: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            Log.d(TAG, "========== printTestTextByIp END (error) ==========")
            Result.failure(Exception("Error inesperado: ${e.message}"))
        } finally {
            // Cerrar recursos
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
     * Envía un texto de prueba a la impresora Bluetooth
     * @param deviceAddress Dirección MAC del dispositivo Bluetooth
     * @param text Texto a imprimir
     * @return Result con éxito o error
     */
    suspend fun printTestText(deviceAddress: String, text: String): Result<String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "========== printTestText START ==========")
        Log.d(TAG, "Device address: $deviceAddress")
        Log.d(TAG, "Text to print: $text")

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            // Obtener el dispositivo Bluetooth
            val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (device == null) {
                Log.e(TAG, "Device not found")
                return@withContext Result.failure(Exception("Dispositivo Bluetooth no encontrado"))
            }

            Log.d(TAG, "Device found: ${device.name}")

            // Crear socket
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            Log.d(TAG, "Socket created")

            // Cancelar discovery para mejorar conexión
            try {
                bluetoothAdapter?.cancelDiscovery()
                Log.d(TAG, "Discovery cancelled")
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not cancel discovery (permission issue): ${e.message}")
            }

            // Conectar
            Log.d(TAG, "Connecting to device...")
            socket.connect()
            Log.d(TAG, " Connected successfully")

            // Obtener output stream
            outputStream = socket.outputStream
            Log.d(TAG, "Output stream obtained")

            // Generar comandos ZPL para Zebra
            val zplCommands = buildZPLTestLabel(text)
            Log.d(TAG, "ZPL commands generated:")
            Log.d(TAG, zplCommands)

            // Enviar comandos
            Log.d(TAG, "Sending commands to printer...")
            outputStream.write(zplCommands.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            Log.d(TAG, " Commands sent successfully")

            // Pequeña pausa para asegurar que se envió todo
            Thread.sleep(500)

            Log.d(TAG, "========== printTestText END (success) ==========")
            Result.success("Impresión exitosa")

        } catch (e: IOException) {
            Log.e(TAG, "IOException: ${e.message}", e)
            Log.d(TAG, "========== printTestText END (error) ==========")
            Result.failure(Exception("Error de conexión: ${e.message}"))
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            Log.d(TAG, "========== printTestText END (error) ==========")
            Result.failure(Exception("Permisos de Bluetooth no concedidos"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            Log.d(TAG, "========== printTestText END (error) ==========")
            Result.failure(Exception("Error inesperado: ${e.message}"))
        } finally {
            // Cerrar recursos
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
     * Genera comandos ZPL para imprimir una etiqueta de prueba
     * Optimizado para Zebra ZQ511 (impresora térmica portátil 3" - 72mm)
     * Configuración: 72mm ancho, estilo recibo de venta (tipo Walmart)
     * Fuente compacta para facturas: ~42-48 caracteres por línea
     */
    private fun buildZPLTestLabel(text: String): String {
        // Dividir el texto en líneas
        val lines = text.split("\n")

        // Construir comandos ZPL para estilo recibo
        // Configuración exacta para coincidir con virtual printer:
        // - 48 caracteres por línea (72mm)
        // - Fuente monospace tipo Courier 11pt
        // - Line height 14pt
        val commands = StringBuilder()
        commands.append("^XA\n")  // Inicio de formato
        commands.append("^CI28\n")  // Codificación UTF-8 para tildes y caracteres especiales
        commands.append("^PW576\n")  // Ancho de impresión (576 dots para 72mm a 203 DPI)
        commands.append("^LL${80 + (lines.size * 28)}\n")  // Largo dinámico según líneas
        commands.append("^CF0,24\n")  // Fuente por defecto: altura 24 (100% tamaño base)

        // Agregar cada línea con fuente explícita al 100%
        var yPosition = 15
        lines.forEach { line ->
            if (line.isNotBlank()) {
                commands.append("^FO3,$yPosition^A0N,24,12^FD$line^FS\n")  // 100%: altura 24, ancho 12
                yPosition += 28  // Line spacing para 100%
            }
        }

        commands.append("^XZ")  // Fin de formato

        return commands.toString()
    }
}
