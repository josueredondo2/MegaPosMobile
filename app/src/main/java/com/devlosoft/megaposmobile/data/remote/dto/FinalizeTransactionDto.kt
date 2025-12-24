package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FinalizeTransactionRequestDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("workstationId") val workstationId: String,
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("dataphoneData") val dataphoneData: DataphoneDataDto? = null
)

/**
 * DTO con los datos del datáfono para enviar al API de megapos.
 * Mapea a DataphonePaymentInput en el backend.
 */
data class DataphoneDataDto(
    @SerializedName("autorizacion") val autorizacion: String?,
    @SerializedName("panmasked") val panmasked: String?,
    @SerializedName("cardholder") val cardholder: String?,
    @SerializedName("terminalid") val terminalid: String,
    @SerializedName("recibo") val recibo: String?,
    @SerializedName("rrn") val rrn: String?,
    @SerializedName("stan") val stan: String?,
    @SerializedName("ticket") val ticket: String?,
    @SerializedName("totalAmount") val totalAmount: String?
)

data class FinalizeTransactionResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("transactionId") val transactionId: String?
)
