package com.devlosoft.megaposmobile.core.dataphone

import android.util.Log
import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

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
            } catch (e: Exception) {
                Log.e(TAG, "Error processing payment", e)
                Result.failure(Exception("Error al comunicarse con el datáfono: ${e.message}"))
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
            } catch (e: Exception) {
                Result.failure(Exception("No se pudo conectar con el datáfono: ${e.message}"))
            }
        }
}
