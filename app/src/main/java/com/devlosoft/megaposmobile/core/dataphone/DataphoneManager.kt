package com.devlosoft.megaposmobile.core.dataphone

import android.content.Context
import android.content.Intent
import android.util.Log
import com.devlosoft.megaposmobile.MainActivity
import com.devlosoft.megaposmobile.BuildConfig
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.domain.model.DatafonoProvider
import com.devlosoft.megaposmobile.domain.model.DataphoneCloseResult
import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager/Orquestador para operaciones con el datáfono.
 * Lee la configuración de Room y delega al driver correcto.
 */
@Singleton
class DataphoneManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverConfigDao: ServerConfigDao,
    private val driverFactory: DataphoneDriverFactory
) {
    companion object {
        private const val TAG = "DataphoneManager"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)  // PAX puede tardar en procesar
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Procesa un pago en el datáfono.
     * @param amount Monto en colones (sin decimales)
     * @param useSimulation Forzar modo simulación (para desarrollo)
     * @return Resultado del pago
     */
    suspend fun processPayment(amount: Long, useSimulation: Boolean = false): Result<DataphonePaymentResult> {
        // Usar simulación si está en modo desarrollo o se fuerza
        if (useSimulation || BuildConfig.DEVELOPMENT_MODE) {
            Log.d(TAG, "Using simulation mode for payment of $amount")
            return simulatePayment(amount)
        }

        val config = serverConfigDao.getActiveServerConfigSync()
        if (config == null) {
            Log.e(TAG, "No configuration found")
            return Result.failure(Exception("Configuración del servidor no encontrada"))
        }

        if (config.datafonUrl.isBlank()) {
            Log.w(TAG, "Dataphone URL not configured, falling back to simulation")
            return simulatePayment(amount)
        }

        val provider = DatafonoProvider.fromString(config.datafonoProvider)
        Log.d(TAG, "Processing payment: amount=$amount, provider=$provider, url=${config.datafonUrl}")

        val driver = driverFactory.createDriver(provider)

        val service = HttpDataphoneService(
            baseUrl = config.datafonUrl,
            driver = driver,
            httpClient = httpClient
        )

        val result = service.processPayment(amount)

        // Traer la app al frente después del pago
        bringAppToFront()

        return result
    }

    /**
     * Simula un pago para desarrollo/pruebas.
     */
    private suspend fun simulatePayment(amount: Long): Result<DataphonePaymentResult> {
        Log.d(TAG, "Simulating payment for amount: $amount")
        delay(500)  // Simular tiempo de respuesta

        return Result.success(
            DataphonePaymentResult(
                success = true,
                respcode = "00",
                authorizationCode = "SIM${(100000..999999).random()}",
                panmasked = "****${(1000..9999).random()}",
                cardholder = "CLIENTE SIMULADO",
                issuername = "VISA",
                terminalid = "SIMULADOR",
                receiptNumber = String.format("%06d", (1..999999).random()),
                rrn = "SIM${System.currentTimeMillis() % 1000000000}",
                stan = String.format("%06d", (1..999999).random()),
                ticket = "SIMULACION DE PAGO\n" +
                        "==================\n" +
                        "MONTO: CRC ${amount}.00\n" +
                        "VALIDO SIN FIRMA\n" +
                        "==================",
                totalAmount = "CRC${amount * 100}.00",
                errorMessage = null
            )
        )
    }

    /**
     * Prueba la conexión con el datáfono.
     */
    suspend fun testConnection(): Result<String> {
        val config = serverConfigDao.getActiveServerConfigSync()
            ?: return Result.failure(Exception("Configuración no encontrada"))

        if (config.datafonUrl.isBlank()) {
            return Result.failure(Exception("URL del datáfono no configurada"))
        }

        val provider = DatafonoProvider.fromString(config.datafonoProvider)
        val driver = driverFactory.createDriver(provider)

        val service = HttpDataphoneService(
            baseUrl = config.datafonUrl,
            driver = driver,
            httpClient = httpClient
        )

        return service.testConnection()
    }

    /**
     * Ejecuta el cierre de lote del datáfono.
     * @param useSimulation Forzar modo simulación (para desarrollo)
     * @return Resultado del cierre con totales
     */
    suspend fun closeDataphone(useSimulation: Boolean = false): Result<DataphoneCloseResult> {
        // Usar simulación si está en modo desarrollo o se fuerza
        if (useSimulation || BuildConfig.DEVELOPMENT_MODE) {
            Log.d(TAG, "Using simulation mode for dataphone close")
            return simulateClose()
        }

        val config = serverConfigDao.getActiveServerConfigSync()
        if (config == null) {
            Log.e(TAG, "No configuration found")
            return Result.failure(Exception("Configuración del servidor no encontrada"))
        }

        if (config.datafonUrl.isBlank()) {
            Log.w(TAG, "Dataphone URL not configured, falling back to simulation")
            return simulateClose()
        }

        val provider = DatafonoProvider.fromString(config.datafonoProvider)
        Log.d(TAG, "Closing dataphone: provider=$provider, url=${config.datafonUrl}")

        val driver = driverFactory.createDriver(provider)

        val service = HttpDataphoneService(
            baseUrl = config.datafonUrl,
            driver = driver,
            httpClient = httpClient
        )

        val result = service.closeDataphone()

        // Traer la app al frente después del cierre
        bringAppToFront()

        return result
    }

    /**
     * Simula un cierre para desarrollo/pruebas.
     */
    private suspend fun simulateClose(): Result<DataphoneCloseResult> {
        Log.d(TAG, "Simulating dataphone close")
        delay(500)  // Simular tiempo de respuesta

        return Result.success(
            DataphoneCloseResult(
                success = true,
                terminal = "SIMULADOR",
                batchNumber = String.format("%06d", (1..999999).random()),
                salesCount = (1..10).random(),
                salesTotal = (10000..500000).random().toDouble(),
                reversalsCount = 0,
                reversalsTotal = 0.0,
                netTotal = (10000..500000).random().toDouble(),
                ticket = "SIMULACION DE CIERRE\n" +
                        "==================\n" +
                        "CIERRE COMPLETADO\n" +
                        "==================",
                errorMessage = null
            )
        )
    }

    /**
     * Trae la app al frente después de que el datáfono complete el pago.
     */
    private fun bringAppToFront() {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "App brought to front after payment")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring app to front", e)
        }
    }
}
