package com.devlosoft.megaposmobile.core.printer

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
     * @return Texto formateado listo para impresión
     */
    fun buildPendingTransactionReceipt(
        userName: String,
        totalItems: Int,
        subtotal: Double,
        transactionId: String,
        customerIdentification: String
    ): String {
        val currentDateTime = dateFormat.format(Date())
        val formattedSubtotal = currencyFormat.format(subtotal)

        return buildString {
            // Encabezado
            appendLine(centerText("Megasuper Paraiso"))
            appendLine(centerText("CORPORACION MEGASUPER S.A."))
            appendLine()
            appendLine(centerText("Transacción en espera"))
            appendLine()

            // Información del cliente y cajero
            appendLine("Ced: $customerIdentification")
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
     * Centra un texto dentro del ancho de línea especificado.
     */
    private fun centerText(text: String): String {
        if (text.length >= LINE_WIDTH) return text
        val padding = (LINE_WIDTH - text.length) / 2
        return " ".repeat(padding) + text
    }
}
