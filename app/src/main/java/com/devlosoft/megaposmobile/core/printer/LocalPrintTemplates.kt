package com.devlosoft.megaposmobile.core.printer

import com.devlosoft.megaposmobile.data.remote.dto.ClosedTransactionSummaryDto
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera plantillas de texto pre-formateado para impresión local.
 * Estas plantillas no requieren llamadas al backend.
 *
 * El texto se formatea para impresoras térmicas con 48 caracteres de ancho.
 */
object LocalPrintTemplates {

    private const val LINE_WIDTH = 48
    private const val SEPARATOR = "------------------------------------------------"

    private val currencyFormat = NumberFormat.getNumberInstance(Locale("es", "CR")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("es", "CR"))

    /**
     * Genera el texto para el comprobante de transacción pausada (en espera).
     *
     * @param userName Nombre del cajero/usuario logueado
     * @param totalItems Cantidad total de artículos en la transacción
     * @param subtotal Subtotal de la transacción
     * @param transactionId ID de la transacción
     * @param customerIdentification Cédula/identificación del cliente
     * @param businessUnitName Nombre de la unidad de negocio
     * @return Texto formateado listo para impresión
     */
    fun buildPendingTransactionReceipt(
        userName: String,
        totalItems: Int,
        subtotal: Double,
        transactionId: String,
        businessUnitName: String = "Megasuper"
    ): String {
        val currentDateTime = dateFormat.format(Date())
        val formattedSubtotal = currencyFormat.format(subtotal)

        return buildString {
            // Encabezado
            appendLine(centerText(businessUnitName))
            appendLine(centerText("CORPORACION MEGASUPER S.A."))
            appendLine()
            appendLine(centerText("Transacción en espera"))
            appendLine()

            // Información del cliente y cajero
            appendLine("Ced: 3-101-052164")
            appendLine(currentDateTime)
            appendLine("Cajero: $userName")
            appendLine(SEPARATOR)

            // Detalles de la transacción
            appendLine("Articulos: $totalItems")
            appendLine("Subtotal: $formattedSubtotal")
            appendLine("ID Transacción: $transactionId")
            appendLine()
        }
    }

    /**
     * Genera el texto para el comprobante de cierre de datáfono/PinPad.
     *
     * @param userName Nombre del cajero/usuario que realizó el cierre
     * @param terminalId Código del terminal/datáfono
     * @param closeDate Fecha y hora del cierre
     * @param salesCount Cantidad de ventas
     * @param salesTotal Total de ventas
     * @param reversalsCount Cantidad de reversiones
     * @param reversalsTotal Total de reversiones
     * @param netTotal Total neto
     * @param voucher Voucher del PAX (detalle del cierre)
     * @param businessUnitName Nombre de la unidad de negocio
     * @param closedTransactions Lista de transacciones cerradas para imprimir al final
     * @return Texto formateado listo para impresión
     */
    fun buildDataphoneCloseReceipt(
        userName: String,
        terminalId: String,
        closeDate: String,
        salesCount: Int,
        salesTotal: Double,
        reversalsCount: Int,
        reversalsTotal: Double,
        netTotal: Double,
        voucher: String?,
        businessUnitName: String = "Megasuper",
        closedTransactions: List<ClosedTransactionSummaryDto>? = null
    ): String {
        val formattedSalesTotal = currencyFormat.format(salesTotal)
        val formattedReversalsTotal = currencyFormat.format(reversalsTotal)
        val formattedNetTotal = currencyFormat.format(netTotal)

        return buildString {
            // Encabezado
            appendLine(centerText("CORPORACION MEGASUPER S.A."))
            appendLine(centerText(businessUnitName))
            appendLine()
            appendLine(centerText("*** Cierre de Datafono BAC ***"))
            appendLine()
            appendLine(SEPARATOR)

            // Información del cierre
            appendLine("Usuario: $userName")
            appendLine("Terminal: $terminalId")
            appendLine("Fecha: $closeDate")
            appendLine(SEPARATOR)

            // Resumen de totales
            appendLine()
            appendLine(formatLabelValue("Ventas:", "$salesCount"))
            appendLine(formatLabelValue("Total Ventas:", formattedSalesTotal))
            appendLine(formatLabelValue("Reversiones:", "$reversalsCount"))
            appendLine(formatLabelValue("Total Reversiones:", formattedReversalsTotal))
            appendLine(SEPARATOR)
            appendLine(formatLabelValue("TOTAL NETO:", formattedNetTotal))
            appendLine(SEPARATOR)

            // Detalle de transacciones cerradas
            if (!closedTransactions.isNullOrEmpty()) {
                appendLine()
                appendLine(centerText("DETALLE DE TRANSACCIONES"))
                appendLine(SEPARATOR)
                appendLine("Autorizacion        Tarjeta         Total")
                appendLine(SEPARATOR)
                for (txn in closedTransactions) {
                    val authCode = txn.authorizationId ?: "------"
                    val cardCode = txn.cardNumber ?: "----"
                    val amountFormatted = "CRC ${currencyFormat.format(txn.amount)}"
                    // Línea 1: ID y Total
                    appendLine(formatLabelValue(txn.transactionId.trim(), amountFormatted))
                    // Línea 2: Autorización y código de tarjeta
                    appendLine("$authCode      $cardCode")
                    appendLine()
                }
                appendLine(SEPARATOR)
            }

            // Espacio para firma del tesorero
            appendLine()
            appendLine()
            appendLine(centerText("________________________"))
            appendLine(centerText("Tesorero"))
            appendLine()
            appendLine()
        }
    }

    /**
     * Centra un texto dentro del ancho de línea especificado.
     */
    private fun centerText(text: String): String {
        if (text.length >= LINE_WIDTH) return text
        val padding = (LINE_WIDTH - text.length) / 2
        return " ".repeat(padding) + text
    }

    /**
     * Formatea una etiqueta y valor alineados.
     */
    private fun formatLabelValue(label: String, value: String): String {
        val spaces = LINE_WIDTH - label.length - value.length
        return if (spaces > 0) {
            label + " ".repeat(spaces) + value
        } else {
            "$label $value"
        }
    }
}
