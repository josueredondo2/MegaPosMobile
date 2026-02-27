package com.devlosoft.megaposmobile.core.dataphone

import android.app.Activity
import android.content.Intent
import android.util.Log
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private var activity: Activity
) : DataphoneService {

    companion object {
        private const val TAG = "EmbeddedDataphoneSvc"
        private const val SMARTPOS_PACKAGE = "com.kinpos.BASEA920"
    }

    private var pendingPaymentDeferred: CompletableDeferred<Result<DataphonePaymentResult>>? = null
    private var pendingCloseDeferred: CompletableDeferred<Result<DataphoneCloseResult>>? = null

    /**
     * Actualiza la referencia de Activity sin perder los Deferred pendientes.
     */
    fun updateActivity(activity: Activity) {
        this.activity = activity
    }

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
            kpInvocador.KP_Sale("", "", "", amountInCents, 0, 0, "", "0188", true)

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
        Log.d(TAG, "handleActivityResult: requestCode=$requestCode, resultCode=$resultCode, " +
                "pendingPayment=${pendingPaymentDeferred != null}, pendingClose=${pendingCloseDeferred != null}")

        if (data?.extras != null) {
            val keys = data.extras!!.keySet().joinToString()
            Log.d(TAG, "Intent extras keys: $keys")
        } else {
            Log.w(TAG, "Intent data or extras is null")
        }

        try {
            val kpInvocador = KP_Invocador(SMARTPOS_PACKAGE, activity)
            val resultado = kpInvocador.getResults(requestCode, resultCode, data)

            if (resultado != null) {
                Log.d(TAG, "Trans_Results class: ${resultado::class.java.name}")
            }

            // Sale result
            if (requestCode == KP_Invocador.PROCESSREQUESTSALE && pendingPaymentDeferred != null) {
                handleSaleResult(resultCode, data, resultado)
                return
            }

            // Close result - route by pending deferred since KP_Close uses a different requestCode
            if (pendingCloseDeferred != null) {
                Log.d(TAG, "Routing to close handler for requestCode=$requestCode")
                handleCloseResult(resultCode, data)
                return
            }

            // Fallback: try payment if deferred exists
            if (pendingPaymentDeferred != null) {
                Log.d(TAG, "Routing to sale handler (fallback) for requestCode=$requestCode")
                handleSaleResult(resultCode, data, resultado)
                return
            }

            Log.w(TAG, "Unhandled requestCode: $requestCode (no pending deferreds)")
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
        val authorization = extras?.getString("AUTORIZATION") ?: ""
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
        val totalField = extras?.getString("TOTAL") ?: ""

        Log.d(TAG, "Close result: respCode=$respCode, TOTAL=$totalField")

        val success = respCode == "00"

        if (success) {
            val closeResult = parseCloseTotalField(totalField)
            deferred.complete(Result.success(closeResult))
        } else {
            deferred.complete(Result.failure(
                Exception(PaxBacDriver.getResponseMessage(respCode))
            ))
        }
    }

    /**
     * Parsea el campo TOTAL del Intent de cierre.
     *
     * Formato CSV por acquirer, separados por '|':
     *   terminalId,moneda,salesCount,salesTotalCents,merchantId,batchNumber,?,reversalsCount,reversalsTotalCents
     *
     * Ejemplo:
     *   EMVPOS29,COLONES   ,0001,000000050000,000000011813003,000003,null,0000,000000000000|
     */
    private fun parseCloseTotalField(totalField: String): DataphoneCloseResult {
        var terminalId = ""
        var merchantId = ""
        var batchNumber = ""
        var salesCount = 0
        var salesTotal = 0.0
        var reversalsCount = 0
        var reversalsTotal = 0.0

        if (totalField.isNotEmpty()) {
            val acquirers = totalField.split("|").filter { it.isNotBlank() }

            for (acquirer in acquirers) {
                val parts = acquirer.split(",").map { it.trim() }
                if (parts.size < 9) {
                    Log.w(TAG, "TOTAL acquirer has ${parts.size} fields, expected 9: $acquirer")
                    continue
                }

                if (terminalId.isEmpty()) terminalId = parts[0]
                if (merchantId.isEmpty()) merchantId = parts[4]
                if (batchNumber.isEmpty()) batchNumber = parts[5]

                salesCount += parts[2].toIntOrNull() ?: 0
                salesTotal += (parts[3].toLongOrNull() ?: 0L) / 100.0
                reversalsCount += parts[7].toIntOrNull() ?: 0
                reversalsTotal += (parts[8].toLongOrNull() ?: 0L) / 100.0
            }
        }

        val netTotal = salesTotal - reversalsTotal

        // Build a synthetic ticket matching the format the backend ParseCloseTicket expects:
        //   "TERMINAL ID LOGOSALE {merchantId}"
        //   "Fecha: dd/MM/yyyy Hora: HH:mm Lote: {batch}"
        //   "VENTAS {count} CRC{amount}"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "CR"))
        val now = Date()
        val amountSymbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
        val amountFormat = DecimalFormat("#,##0.00", amountSymbols)

        val syntheticTicket = buildString {
            append("TERMINAL ID LOGOSALE $merchantId\\n")
            append("Fecha: ${dateFormat.format(now)} Hora: ${timeFormat.format(now)} Lote: $batchNumber\\n")
            append("VENTAS %04d CRC%s".format(salesCount, amountFormat.format(salesTotal)))
        }

        Log.d(TAG, "Parsed close: terminal=$terminalId, batch=$batchNumber, " +
                "sales=$salesCount/$salesTotal, reversals=$reversalsCount/$reversalsTotal, net=$netTotal")

        return DataphoneCloseResult(
            success = true,
            terminal = terminalId,
            batchNumber = batchNumber,
            salesCount = salesCount,
            salesTotal = salesTotal,
            reversalsCount = reversalsCount,
            reversalsTotal = reversalsTotal,
            netTotal = netTotal,
            ticket = syntheticTicket,
            errorMessage = null
        )
    }
}
