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

    companion object {
        /**
         * Códigos de respuesta del PAX según documentación BAC Credomatic.
         * Solo el código "00" es APROBADO.
         */
        private val RESPONSE_CODES = mapOf(
            "00" to "Aprobada",
            "01" to "Consulte verbal",
            "02" to "Consulte verbal",
            "03" to "Comercio inválido",
            "04" to "Capture tarjeta",
            "05" to "Denegada",
            "09" to "Aceptado",
            "10" to "Aprobado parcialmente",
            "12" to "Transacción inválida",
            "13" to "Cantidad inválida",
            "14" to "Tarjeta inválida",
            "19" to "Reintente transacción",
            "21" to "Sin transacciones",
            "25" to "Reintente transacción",
            "41" to "Retener tarjeta",
            "43" to "Retener tarjeta",
            "51" to "Fondos insuficientes",
            "54" to "Tarjeta vencida",
            "57" to "Transacción no permitida",
            "58" to "Transacción no permitida",
            "60" to "Denegada",
            "61" to "Denegada",
            "62" to "Denegada",
            "63" to "Denegada",
            "75" to "Denegada",
            "78" to "Transacción no encontrada",
            "79" to "Lote ya abierto",
            "80" to "Error en número de lote",
            "85" to "Lote no existe",
            "89" to "Terminal inválido",
            "94" to "Transacción duplicada",
            "95" to "Espere transmisión",
            "96" to "Error en sistema",
            "N7" to "Código de seguridad inválido",
            "CE" to "Error de comunicación",
            "ID" to "Reintente cierre",
            "IA" to "Monto inválido",
            "IT" to "Terminal inválida",
            "IR" to "Tipo de mensaje inválido",
            "NA" to "Sistema no disponible",
            "TO" to "Tiempo agotado, reintente",
            "ND" to "Reintente transacción",
            "NV" to "Transacción inválida",
            "TR" to "Reintente transacción",
            "YP" to "Transacción ya procesada",
            "BI" to "Error en lectura de tarjeta",
            "DB" to "Error acceso a base de datos",
            "WE" to "Error interno del servicio",
            "RC" to "Rechazado por sistema de recarga",
            "RE" to "Sistema de recarga no disponible",
            "NE" to "Número de teléfono inválido",
            "EV" to "Error de validación",
            "CD" to "Denegado por Coinca",
            "BN" to "Billetera no existe",
            "OV" to "OTP vencido",
            "OI" to "OTP inválido",
            "MI" to "Moneda inválida",
            "OC" to "OTP cancelado",
            "OE" to "OTP expirado",
            "X1" to "Fondos insuficientes",
            "X2" to "Supera límite de billetera",
            "X3" to "Monto no autorizado",
            "X4" to "No acepta anulación"
        )

        /**
         * Obtiene el mensaje descriptivo para un código de respuesta.
         */
        fun getResponseMessage(code: String?): String {
            if (code == null) return "Error desconocido"
            return RESPONSE_CODES[code] ?: "Error: código $code"
        }
    }

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
                    getResponseMessage(paxResponse.respcode) else null
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
