package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta JSON del PAX A920 al ejecutar cierre de lote.
 * Endpoint PAX: GET http://{ip}:8080/cierre?tamanoLinea=42&delimitador=|
 */
data class PaxCloseResponseDto(
    @SerializedName("BASE_AMOUNT")
    val baseAmount: String? = null,

    @SerializedName("CARDHOLDER")
    val cardholder: String? = null,

    @SerializedName("RECIBO")
    val recibo: String? = null,

    @SerializedName("STAN")
    val stan: String? = null,

    @SerializedName("TAX_AMOUNT")
    val taxAmount: String? = null,

    @SerializedName("TICKET")
    val ticket: String? = null,

    @SerializedName("TIP_AMOUNT")
    val tipAmount: String? = null,

    @SerializedName("TOTAL_AMOUNT")
    val totalAmount: String? = null,

    @SerializedName("TxnId")
    val txnId: String? = null
)
