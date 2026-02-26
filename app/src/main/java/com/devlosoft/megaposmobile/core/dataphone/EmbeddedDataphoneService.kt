package com.devlosoft.megaposmobile.core.dataphone

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.devlosoft.megaposmobile.core.dataphone.drivers.PaxBacDriver
import com.devlosoft.megaposmobile.domain.model.DataphoneCloseResult
import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult
import com.kinpos.kpinvocacion.KP_Invocador
import kotlinx.coroutines.CompletableDeferred

/**
 * Implementación embebida del servicio de datáfono para PAX A920.
 * Usa KP_Invocador (librería BAC Credomatic) que se comunica via Intents
 * con la app mPOS instalada en el mismo dispositivo.
 *
 * Bridge pattern: KP_Invocador usa startActivityForResult/onActivityResult,
 * mientras que nuestro código usa coroutines. CompletableDeferred conecta ambos mundos.
 */
class EmbeddedDataphoneService(
    private val activity: Activity
) : DataphoneService {

    companion object {
        private const val TAG = "EmbeddedDataphoneSvc"
        private const val SMARTPOS_PACKAGE = "com.kinpos.BASEA920"
    }

    private var pendingPaymentDeferred: CompletableDeferred<Result<DataphonePaymentResult>>? = null
    private var pendingCloseDeferred: CompletableDeferred<Result<DataphoneCloseResult>>? = null

    override suspend fun processPayment(amount: Long): Result<DataphonePaymentResult> {
        // Cancel any previous pending operation
        pendingPaymentDeferred?.cancel()

        val deferred = CompletableDeferred<Result<DataphonePaymentResult>>()
        pendingPaymentDeferred = deferred

        try {
            val kpInvocador = KP_Invocador(SMARTPOS_PACKAGE, activity)

            // La librería espera centavos: 100.67 → 10067
            // amount ya viene en colones enteros, multiplicar por 100
            val amountInCents = amount * 100

            Log.d(TAG, "Starting embedded sale: amount=$amount, cents=$amountInCents")

            // KP_Sale(user, password, deviceID, monto, montoTIP, montoTAX, email, codigoMoneda, showMessages)
            // user/password/deviceID se pasan vacíos (no aplican en modo embebido según documentación)
            // codigoMoneda "0188" = CRC (Costa Rica Colones)
            kpInvocador.KP_Sale("", "", "", amountInCents, 0, 0, "", "0188", false)

        } catch (e: Exception) {
            Log.e(TAG, "Error launching KP_Sale", e)
            pendingPaymentDeferred = null
            return Result.failure(Exception("Error al iniciar pago en datáfono: ${e.message}"))
        }

        return try {
            deferred.await()
        } catch (e: Exception) {
            Log.e(TAG, "Payment deferred cancelled or failed", e)
            Result.failure(Exception("Pago cancelado"))
        } finally {
            pendingPaymentDeferred = null
        }
    }

    override suspend fun testConnection(): Result<String> {
        return try {
            val packageInfo = activity.packageManager.getPackageInfo(SMARTPOS_PACKAGE, 0)
            if (packageInfo != null) {
                Result.success("App BAC mPOS encontrada (${packageInfo.versionName})")
            } else {
                Result.failure(Exception("App BAC mPOS no encontrada"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("App BAC mPOS ($SMARTPOS_PACKAGE) no está instalada en el dispositivo"))
        }
    }

    override suspend fun closeDataphone(): Result<DataphoneCloseResult> {
        pendingCloseDeferred?.cancel()

        val deferred = CompletableDeferred<Result<DataphoneCloseResult>>()
        pendingCloseDeferred = deferred

        try {
            val kpInvocador = KP_Invocador(SMARTPOS_PACKAGE, activity)
            Log.d(TAG, "Starting embedded close")
            kpInvocador.KP_Close("", "", "")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching KP_Close", e)
            pendingCloseDeferred = null
            return Result.failure(Exception("Error al iniciar cierre de datáfono: ${e.message}"))
        }

        return try {
            deferred.await()
        } catch (e: Exception) {
            Log.e(TAG, "Close deferred cancelled or failed", e)
            Result.failure(Exception("Cierre cancelado"))
        } finally {
            pendingCloseDeferred = null
        }
    }

    /**
     * Debe llamarse desde Activity.onActivityResult().
     * Parsea el resultado del Intent y completa el Deferred correspondiente.
     */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "handleActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        try {
            val kpInvocador = KP_Invocador(SMARTPOS_PACKAGE, activity)
            val resultado = kpInvocador.getResults(requestCode, resultCode, data)

            // Sale result
            if (requestCode == KP_Invocador.PROCESSREQUESTSALE) {
                handleSaleResult(resultCode, data, resultado)
                return
            }

            // Close result - KP_Close doesn't have a constant, use the remaining deferred
            if (pendingCloseDeferred != null) {
                handleCloseResult(resultCode, data)
                return
            }

            Log.w(TAG, "Unhandled requestCode: $requestCode")
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleActivityResult", e)
            pendingPaymentDeferred?.complete(
                Result.failure(Exception("Error procesando respuesta del datáfono: ${e.message}"))
            )
            pendingCloseDeferred?.complete(
                Result.failure(Exception("Error procesando respuesta del datáfono: ${e.message}"))
            )
        }
    }

    private fun handleSaleResult(resultCode: Int, data: Intent?, resultado: Any?) {
        val deferred = pendingPaymentDeferred
        if (deferred == null) {
            Log.w(TAG, "No pending payment deferred for sale result")
            return
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Sale cancelled or no data: resultCode=$resultCode")
            deferred.complete(Result.failure(Exception("Pago cancelado por el usuario")))
            return
        }

        val extras = data.extras
        val respCode = extras?.getString("RESPCODE") ?: ""
        val authorization = extras?.getString("AUTORIZACION") ?: ""
        val panMasked = extras?.getString("PANMASKED") ?: ""
        val cardHolder = extras?.getString("CARDHOLDER") ?: ""
        val terminalId = extras?.getString("TERMINALID") ?: ""
        val rrn = extras?.getString("RRN")?.trim() ?: ""
        val stan = extras?.getString("STAN") ?: ""
        val recibo = extras?.getLong("RECIBO", 0)?.toString() ?: "0"
        val totalAmount = extras?.getLong("TOTAL_AMOUNT", 0)?.toString() ?: "0"
        val ticket = extras?.getString("TICKET") ?: ""

        Log.d(TAG, "Sale result: respCode=$respCode, auth=$authorization, recibo=$recibo, stan=$stan")

        val success = respCode == "00"
        val result = DataphonePaymentResult(
            success = success,
            respcode = respCode,
            authorizationCode = authorization,
            panmasked = panMasked,
            cardholder = cardHolder,
            issuername = null,
            terminalid = terminalId,
            receiptNumber = recibo,
            rrn = rrn,
            stan = stan,
            ticket = ticket,
            totalAmount = totalAmount,
            errorMessage = if (!success) PaxBacDriver.getResponseMessage(respCode) else null
        )

        if (success) {
            deferred.complete(Result.success(result))
        } else {
            deferred.complete(Result.failure(
                Exception(result.errorMessage ?: "Pago rechazado por el datáfono")
            ))
        }
    }

    private fun handleCloseResult(resultCode: Int, data: Intent?) {
        val deferred = pendingCloseDeferred
        if (deferred == null) {
            Log.w(TAG, "No pending close deferred for close result")
            return
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Close cancelled or no data: resultCode=$resultCode")
            deferred.complete(Result.failure(Exception("Cierre cancelado por el usuario")))
            return
        }

        val extras = data.extras
        val respCode = extras?.getString("RESPCODE") ?: ""
        val ticket = extras?.getString("TICKET") ?: ""
        val terminalId = extras?.getString("TERMINALID") ?: ""

        Log.d(TAG, "Close result: respCode=$respCode, ticket length=${ticket.length}")

        val success = respCode == "00"

        if (success) {
            // Parse the ticket to extract close totals (same format as HTTP response)
            val closeResult = parseCloseTicket(ticket, terminalId)
            deferred.complete(Result.success(closeResult))
        } else {
            deferred.complete(Result.failure(
                Exception(PaxBacDriver.getResponseMessage(respCode))
            ))
        }
    }

    /**
     * Parsea el TICKET del cierre para extraer datos estructurados.
     * Reutiliza la misma lógica que PaxBacDriver pero aplicada al ticket del Intent.
     */
    private fun parseCloseTicket(ticket: String, terminalId: String): DataphoneCloseResult {
        var batchNumber = ""
        var salesCount = 0
        var salesTotal = 0.0

        if (ticket.isNotEmpty()) {
            val normalizedTicket = ticket.replace("\\n", "\n").replace("|", "\n")
            val lines = normalizedTicket.split("\n").filter { it.isNotBlank() }

            for (line in lines) {
                val cleanLine = line.trim().trimStart('s').trim()

                if (cleanLine.contains("Lote:")) {
                    val loteRegex = Regex("Lote:\\s*(\\d+)")
                    loteRegex.find(cleanLine)?.let { match ->
                        batchNumber = match.groupValues[1]
                    }
                }

                if (cleanLine.startsWith("VENTAS")) {
                    val ventasRegex = Regex("VENTAS\\s+(\\d+)\\s+CRC([\\d,\\.]+)")
                    ventasRegex.find(cleanLine)?.let { match ->
                        salesCount = match.groupValues[1].toIntOrNull() ?: 0
                        salesTotal = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                    }
                }
            }
        }

        return DataphoneCloseResult(
            success = true,
            terminal = terminalId,
            batchNumber = batchNumber,
            salesCount = salesCount,
            salesTotal = salesTotal,
            reversalsCount = 0,
            reversalsTotal = 0.0,
            netTotal = salesTotal,
            ticket = ticket,
            errorMessage = null
        )
    }
}
