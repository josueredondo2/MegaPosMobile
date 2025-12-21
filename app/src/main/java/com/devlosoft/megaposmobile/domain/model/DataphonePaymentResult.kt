package com.devlosoft.megaposmobile.domain.model

/**
 * Resultado normalizado del procesamiento del datáfono.
 * Este modelo es agnóstico del adquirente.
 */
data class DataphonePaymentResult(
    val success: Boolean,
    val respcode: String?,
    val autorizacion: String?,
    val panmasked: String?,
    val cardholder: String?,
    val issuername: String?,
    val terminalid: String?,
    val recibo: String?,
    val rrn: String?,
    val stan: String?,
    val ticket: String?,
    val totalAmount: String?,
    val errorMessage: String? = null
)
