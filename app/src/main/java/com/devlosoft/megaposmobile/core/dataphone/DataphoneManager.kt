package com.devlosoft.megaposmobile.core.dataphone

import android.content.Context
import android.content.Intent
import android.util.Log
import com.devlosoft.megaposmobile.MainActivity
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.domain.model.DatafonoProvider
import com.devlosoft.megaposmobile.domain.model.DataphoneCloseResult
import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult
import dagger.hilt.android.qualifiers.ApplicationContext
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
     * @return Resultado del pago
     */
    suspend fun processPayment(amount: Long): Result<DataphonePaymentResult> {
        val config = serverConfigDao.getActiveServerConfigSync()
        if (config == null) {
            Log.e(TAG, "No configuration found")
            return Result.failure(Exception("Configuración del servidor no encontrada"))
        }

        if (config.datafonUrl.isBlank()) {
            Log.e(TAG, "Dataphone URL not configured")
            return Result.failure(Exception("URL del datáfono no configurada"))
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
     * @return Resultado del cierre con totales
     */
    suspend fun closeDataphone(): Result<DataphoneCloseResult> {
        val config = serverConfigDao.getActiveServerConfigSync()
        if (config == null) {
            Log.e(TAG, "No configuration found")
            return Result.failure(Exception("Configuración del servidor no encontrada"))
        }

        if (config.datafonUrl.isBlank()) {
            Log.e(TAG, "Dataphone URL not configured")
            return Result.failure(Exception("URL del datáfono no configurada"))
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
