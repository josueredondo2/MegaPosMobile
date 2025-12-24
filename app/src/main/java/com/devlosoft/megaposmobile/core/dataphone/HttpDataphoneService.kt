package com.devlosoft.megaposmobile.core.dataphone

import android.util.Log
import com.devlosoft.megaposmobile.domain.model.DataphoneCloseResult
import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Implementación HTTP del servicio de datáfono.
 * Realiza llamadas HTTP al datáfono físico.
 */
class HttpDataphoneService(
    private val baseUrl: String,
    private val driver: DataphoneDriver,
    private val httpClient: OkHttpClient
) : DataphoneService {

    companion object {
        private const val TAG = "HttpDataphoneService"
    }

    override suspend fun processPayment(amount: Long): Result<DataphonePaymentResult> =
        withContext(Dispatchers.IO) {
            try {
                val url = driver.buildRequestUrl(baseUrl, amount)
                Log.d(TAG, "Calling dataphone: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Dataphone returned error: ${response.code}")
                    return@withContext Result.failure(
                        Exception("Error de conexión con el datáfono: código ${response.code}")
                    )
                }

                val jsonResponse = response.body?.string()
                if (jsonResponse.isNullOrEmpty()) {
                    Log.e(TAG, "Empty response from dataphone")
                    return@withContext Result.failure(
                        Exception("Respuesta vacía del datáfono")
                    )
                }

                Log.d(TAG, "Dataphone response: $jsonResponse")

                val result = driver.parseResponse(jsonResponse)

                if (result.success) {
                    Log.d(TAG, "Payment successful: auth=${result.autorizacion}")
                    Result.success(result)
                } else {
                    Log.e(TAG, "Payment rejected: ${result.errorMessage}")
                    Result.failure(Exception(result.errorMessage ?: "Pago rechazado por el datáfono"))
                }
            } catch (e: ConnectException) {
                Log.e(TAG, "Connection refused - SmartPos app may not be open", e)
                Result.failure(Exception("No se pudo conectar con el datáfono.\nAsegúrese de que la app SmartPos esté abierta."))
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout - SmartPos app may not be responding", e)
                Result.failure(Exception("Tiempo de espera agotado.\nAsegúrese de que la app SmartPos esté abierta."))
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Unknown host - network issue or wrong IP", e)
                Result.failure(Exception("No se encontró el datáfono.\nVerifique la IP y que esté en la misma red."))
            } catch (e: Exception) {
                Log.e(TAG, "Error processing payment", e)
                val message = when {
                    e.message?.contains("Connection refused", ignoreCase = true) == true ||
                    e.message?.contains("ECONNREFUSED", ignoreCase = true) == true ->
                        "No se pudo conectar con el datáfono.\nAsegúrese de que la app SmartPos esté abierta."
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Tiempo de espera agotado.\nAsegúrese de que la app SmartPos esté abierta."
                    else -> "Error al comunicarse con el datáfono: ${e.message}"
                }
                Result.failure(Exception(message))
            }
        }

    override suspend fun testConnection(): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(baseUrl)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    Result.success("Conexión exitosa con el datáfono")
                } else {
                    Result.failure(Exception("Error de conexión: código ${response.code}"))
                }
            } catch (e: ConnectException) {
                Result.failure(Exception("No se pudo conectar con el datáfono.\nAsegúrese de que la app SmartPos esté abierta."))
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("Tiempo de espera agotado.\nAsegúrese de que la app SmartPos esté abierta."))
            } catch (e: UnknownHostException) {
                Result.failure(Exception("No se encontró el datáfono.\nVerifique la IP y que esté en la misma red."))
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("Connection refused", ignoreCase = true) == true ->
                        "No se pudo conectar con el datáfono.\nAsegúrese de que la app SmartPos esté abierta."
                    else -> "No se pudo conectar con el datáfono: ${e.message}"
                }
                Result.failure(Exception(message))
            }
        }

    override suspend fun closeDataphone(): Result<DataphoneCloseResult> =
        withContext(Dispatchers.IO) {
            try {
                val url = driver.buildCloseUrl(baseUrl)
                Log.d(TAG, "Calling dataphone close: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Dataphone close returned error: ${response.code}")
                    return@withContext Result.failure(
                        Exception("Error de conexión con el datáfono: código ${response.code}")
                    )
                }

                val jsonResponse = response.body?.string()
                if (jsonResponse.isNullOrEmpty()) {
                    Log.e(TAG, "Empty response from dataphone close")
                    return@withContext Result.failure(
                        Exception("Respuesta vacía del datáfono")
                    )
                }

                Log.d(TAG, "Dataphone close response: $jsonResponse")

                val result = driver.parseCloseResponse(jsonResponse)

                if (result.success) {
                    Log.d(TAG, "Close successful: sales=${result.salesCount}, total=${result.salesTotal}")
                    Result.success(result)
                } else {
                    Log.e(TAG, "Close failed: ${result.errorMessage}")
                    Result.failure(Exception(result.errorMessage ?: "Error al cerrar el datáfono"))
                }
            } catch (e: ConnectException) {
                Log.e(TAG, "Connection refused - SmartPos app may not be open", e)
                Result.failure(Exception("No se pudo conectar con el datáfono.\nAsegúrese de que la app SmartPos esté abierta."))
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout - SmartPos app may not be responding", e)
                Result.failure(Exception("Tiempo de espera agotado.\nAsegúrese de que la app SmartPos esté abierta."))
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Unknown host - network issue or wrong IP", e)
                Result.failure(Exception("No se encontró el datáfono.\nVerifique la IP y que esté en la misma red."))
            } catch (e: Exception) {
                Log.e(TAG, "Error closing dataphone", e)
                val message = when {
                    e.message?.contains("Connection refused", ignoreCase = true) == true ||
                    e.message?.contains("ECONNREFUSED", ignoreCase = true) == true ->
                        "No se pudo conectar con el datáfono.\nAsegúrese de que la app SmartPos esté abierta."
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Tiempo de espera agotado.\nAsegúrese de que la app SmartPos esté abierta."
                    else -> "Error al comunicarse con el datáfono: ${e.message}"
                }
                Result.failure(Exception(message))
            }
        }
}
