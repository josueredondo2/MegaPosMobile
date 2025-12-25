package com.devlosoft.megaposmobile.core.dataphone.drivers

import com.devlosoft.megaposmobile.core.dataphone.DataphoneDriver
import com.devlosoft.megaposmobile.data.remote.dto.PaxCloseResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.PaxResponseDto
import com.devlosoft.megaposmobile.domain.model.DatafonoProvider
import com.devlosoft.megaposmobile.domain.model.DataphoneCloseResult
import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale

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
                authorizationCode = paxResponse.autorizacion,
                panmasked = paxResponse.panmasked,
                cardholder = paxResponse.cardholder,
                issuername = paxResponse.issuername,
                terminalid = paxResponse.terminalid,
                receiptNumber = paxResponse.recibo,
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
                authorizationCode = null,
                panmasked = null,
                cardholder = null,
                issuername = null,
                terminalid = null,
                receiptNumber = null,
                rrn = null,
                stan = null,
                ticket = null,
                totalAmount = null,
                errorMessage = "Error al parsear respuesta: ${e.message}"
            )
        }
    }

    override fun buildCloseUrl(baseUrl: String): String {
        // Endpoint de cierre del PAX con formato de linea de 42 caracteres
        return "$baseUrl/cierre?tamanoLinea=42&delimitador=|"
    }

    override fun parseCloseResponse(jsonResponse: String): DataphoneCloseResult {
        return try {
            val paxResponse = gson.fromJson(jsonResponse, PaxCloseResponseDto::class.java)
            val ticketData = parseCloseTicket(paxResponse.ticket)

            DataphoneCloseResult(
                success = true,
                terminal = ticketData.terminalId,
                batchNumber = ticketData.batchNumber,
                salesCount = ticketData.salesCount,
                salesTotal = ticketData.salesTotal,
                reversalsCount = 0,
                reversalsTotal = 0.0,
                netTotal = ticketData.salesTotal,
                ticket = paxResponse.ticket,
                errorMessage = null
            )
        } catch (e: Exception) {
            DataphoneCloseResult(
                success = false,
                terminal = null,
                batchNumber = null,
                salesCount = 0,
                salesTotal = 0.0,
                reversalsCount = 0,
                reversalsTotal = 0.0,
                netTotal = 0.0,
                ticket = null,
                errorMessage = "Error al parsear respuesta de cierre: ${e.message}"
            )
        }
    }

    /**
     * Parsea el TICKET del cierre para extraer datos estructurados.
     * Formato ejemplo del TICKET:
     * "...TERMINAL ID LOGOSALE 000000030683005|...Fecha: 19/12/2025 Hora: 18:43 Lote: 000001|...VENTAS 0002 CRC1,010.00|..."
     */
    private fun parseCloseTicket(ticket: String?): CloseTicketData {
        val result = CloseTicketData()
        if (ticket.isNullOrEmpty()) return result

        // Normalizar delimitadores (el PAX usa \n y | como separadores)
        val normalizedTicket = ticket.replace("\\n", "\n").replace("|", "\n")
        val lines = normalizedTicket.split("\n").filter { it.isNotBlank() }

        for (line in lines) {
            // Limpiar prefijos 's' usados en formato PAX
            val cleanLine = line.trim().trimStart('s').trim()

            // Extraer Terminal ID: "TERMINAL ID LOGOSALE 000000030683005"
            if (cleanLine.contains("TERMINAL ID")) {
                val parts = cleanLine.split(" ").filter { it.isNotBlank() }
                if (parts.size >= 4) {
                    result.terminalId = parts.last()
                }
            }

            // Extraer Lote: "Fecha: 19/12/2025 Hora: 18:43 Lote: 000001"
            if (cleanLine.contains("Lote:")) {
                val loteRegex = Regex("Lote:\\s*(\\d+)")
                loteRegex.find(cleanLine)?.let { match ->
                    result.batchNumber = match.groupValues[1]
                }
            }

            // Extraer VENTAS: "VENTAS 0002 CRC1,010.00"
            if (cleanLine.startsWith("VENTAS")) {
                val ventasRegex = Regex("VENTAS\\s+(\\d+)\\s+CRC([\\d,\\.]+)")
                ventasRegex.find(cleanLine)?.let { match ->
                    result.salesCount = match.groupValues[1].toIntOrNull() ?: 0
                    result.salesTotal = parseCrcAmount(match.groupValues[2])
                }
            }
        }

        return result
    }

    /**
     * Parsea monto en formato CRC: "1,010.00" -> 1010.00
     */
    private fun parseCrcAmount(amount: String): Double {
        val cleanAmount = amount.replace(",", "")
        return cleanAmount.toDoubleOrNull() ?: 0.0
    }

    /**
     * Datos extraidos del TICKET de cierre
     */
    private data class CloseTicketData(
        var terminalId: String = "",
        var batchNumber: String = "",
        var salesCount: Int = 0,
        var salesTotal: Double = 0.0
    )
}
