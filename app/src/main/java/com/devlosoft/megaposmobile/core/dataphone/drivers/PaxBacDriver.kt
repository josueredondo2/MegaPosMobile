package com.devlosoft.megaposmobile.core.dataphone.drivers

import com.devlosoft.megaposmobile.core.dataphone.DataphoneDriver
import com.devlosoft.megaposmobile.data.remote.dto.PaxResponseDto
import com.devlosoft.megaposmobile.domain.model.DatafonoProvider
import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult
import com.google.gson.Gson

/**
 * Driver para datáfonos PAX A920 de BAC Credomatic.
 */
class PaxBacDriver : DataphoneDriver {

    private val gson = Gson()

    override fun getProvider(): DatafonoProvider = DatafonoProvider.PAX_BAC

    override fun buildRequestUrl(baseUrl: String, amount: Long): String {
        // El PAX requiere el monto multiplicado por 100
        // Ejemplo: 1000 colones = 100000
        val paxAmount = amount * 100
        return "$baseUrl/venta?monto=$paxAmount"
    }

    override fun parseResponse(jsonResponse: String): DataphonePaymentResult {
        return try {
            val paxResponse = gson.fromJson(jsonResponse, PaxResponseDto::class.java)

            DataphonePaymentResult(
                success = paxResponse.respcode == "00",
                respcode = paxResponse.respcode,
                autorizacion = paxResponse.autorizacion,
                panmasked = paxResponse.panmasked,
                cardholder = paxResponse.cardholder,
                issuername = paxResponse.issuername,
                terminalid = paxResponse.terminalid,
                recibo = paxResponse.recibo,
                rrn = paxResponse.rrn?.trim(),
                stan = paxResponse.stan,
                ticket = paxResponse.ticket,
                totalAmount = paxResponse.totalAmount,
                errorMessage = if (paxResponse.respcode != "00")
                    "Transacción rechazada: código ${paxResponse.respcode}" else null
            )
        } catch (e: Exception) {
            DataphonePaymentResult(
                success = false,
                respcode = null,
                autorizacion = null,
                panmasked = null,
                cardholder = null,
                issuername = null,
                terminalid = null,
                recibo = null,
                rrn = null,
                stan = null,
                ticket = null,
                totalAmount = null,
                errorMessage = "Error al parsear respuesta: ${e.message}"
            )
        }
    }
}
