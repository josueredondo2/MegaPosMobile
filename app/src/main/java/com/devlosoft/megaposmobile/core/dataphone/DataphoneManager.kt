package com.devlosoft.megaposmobile.core.dataphone

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
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
 * Rutea entre EmbeddedDataphoneService (PAX) y HttpDataphoneService (ZEBRA)
 * según la configuración de readerBrand.
 */
@Singleton
class DataphoneManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val serverConfigDao: ServerConfigDao,
    private val driverFactory: DataphoneDriverFactory
) {
    companion object {
        private const val TAG = "DataphoneManager"
        private const val SMARTPOS_PACKAGE = "com.kinpos.BASEA920"
        private const val MEGAPOS_PACKAGE = "com.devlosoft.megaposmobile"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var activity: Activity? = null
    private var embeddedService: EmbeddedDataphoneService? = null

    /**
     * Conecta la Activity para el servicio embebido.
     * Llamar desde Activity.onCreate() y onResume().
     */
    fun setActivity(activity: Activity) {
        this.activity = activity
        this.embeddedService = EmbeddedDataphoneService(activity)
        Log.d(TAG, "Activity set for embedded dataphone service")
    }

    /**
     * Limpia la referencia a la Activity para evitar memory leaks.
     * Llamar desde Activity.onDestroy().
     */
    fun clearActivity() {
        this.activity = null
        this.embeddedService = null
        Log.d(TAG, "Activity cleared")
    }

    /**
     * Reenvía el resultado de onActivityResult al servicio embebido.
     * Llamar desde Activity.onActivityResult().
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        embeddedService?.handleActivityResult(requestCode, resultCode, data)
            ?: Log.w(TAG, "handleActivityResult called but no embedded service available")
    }

    private fun isPaxEmbedded(readerBrand: String): Boolean {
        return readerBrand.equals("PAX", ignoreCase = true)
    }

    /**
     * Procesa un pago en el datáfono.
     * PAX → embebido (KP_Invocador), ZEBRA → HTTP
     */
    suspend fun processPayment(amount: Long): Result<DataphonePaymentResult> {
        val config = serverConfigDao.getActiveServerConfigSync()
        if (config == null) {
            Log.e(TAG, "No configuration found")
            return Result.failure(Exception("Configuración del servidor no encontrada"))
        }

        if (isPaxEmbedded(config.readerBrand)) {
            val service = embeddedService
                ?: return Result.failure(Exception("Servicio embebido no disponible. Reinicie la aplicación."))

            Log.d(TAG, "Processing payment via EMBEDDED: amount=$amount")
            return service.processPayment(amount)
        }

        // HTTP path (ZEBRA)
        if (config.datafonUrl.isBlank()) {
            Log.e(TAG, "Dataphone URL not configured")
            return Result.failure(Exception("URL del datáfono no configurada"))
        }

        val provider = DatafonoProvider.fromString(config.datafonoProvider)
        Log.d(TAG, "Processing payment via HTTP: amount=$amount, provider=$provider, url=${config.datafonUrl}")

        val driver = driverFactory.createDriver(provider)

        val service = HttpDataphoneService(
            baseUrl = config.datafonUrl,
            driver = driver,
            httpClient = httpClient
        )

        launchApp(SMARTPOS_PACKAGE)
        val result = service.processPayment(amount)
        launchApp(MEGAPOS_PACKAGE)

        return result
    }

    /**
     * Prueba la conexión con el datáfono.
     * PAX → verifica que la app BAC esté instalada, ZEBRA → HTTP ping
     */
    suspend fun testConnection(): Result<String> {
        val config = serverConfigDao.getActiveServerConfigSync()
            ?: return Result.failure(Exception("Configuración no encontrada"))

        if (isPaxEmbedded(config.readerBrand)) {
            val service = embeddedService
                ?: return Result.failure(Exception("Servicio embebido no disponible. Reinicie la aplicación."))
            return service.testConnection()
        }

        // HTTP path (ZEBRA)
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
     * PAX → embebido (KP_Invocador), ZEBRA → HTTP
     */
    suspend fun closeDataphone(): Result<DataphoneCloseResult> {
        val config = serverConfigDao.getActiveServerConfigSync()
        if (config == null) {
            Log.e(TAG, "No configuration found")
            return Result.failure(Exception("Configuración del servidor no encontrada"))
        }

        if (isPaxEmbedded(config.readerBrand)) {
            val service = embeddedService
                ?: return Result.failure(Exception("Servicio embebido no disponible. Reinicie la aplicación."))

            Log.d(TAG, "Closing dataphone via EMBEDDED")
            return service.closeDataphone()
        }

        // HTTP path (ZEBRA)
        if (config.datafonUrl.isBlank()) {
            Log.e(TAG, "Dataphone URL not configured")
            return Result.failure(Exception("URL del datáfono no configurada"))
        }

        val provider = DatafonoProvider.fromString(config.datafonoProvider)
        Log.d(TAG, "Closing dataphone via HTTP: provider=$provider, url=${config.datafonUrl}")

        val driver = driverFactory.createDriver(provider)

        val service = HttpDataphoneService(
            baseUrl = config.datafonUrl,
            driver = driver,
            httpClient = httpClient
        )

        launchApp(SMARTPOS_PACKAGE)
        val result = service.closeDataphone()
        launchApp(MEGAPOS_PACKAGE)

        return result
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Launched $packageName")
            } else {
                Log.w(TAG, "Package $packageName not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
        }
    }
}
