package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta del datáfono PAX A920.
 * Mapea los campos JSON que devuelve el WebService del PAX.
 */
data class PaxResponseDto(
    @SerializedName("RESPCODE") val respcode: String?,
    @SerializedName("AUTORIZACION") val autorizacion: String?,
    @SerializedName("PANMASKED") val panmasked: String?,
    @SerializedName("CARDHOLDER") val cardholder: String?,
    @SerializedName("ISSUERNAME") val issuername: String?,
    @SerializedName("TERMINALID") val terminalid: String?,
    @SerializedName("RECIBO") val recibo: String?,
    @SerializedName("RRN") val rrn: String?,
    @SerializedName("STAN") val stan: String?,
    @SerializedName("TICKET") val ticket: String?,
    @SerializedName("TOTAL_AMOUNT") val totalAmount: String?,
    @SerializedName("AID") val aid: String?,
    @SerializedName("APP_LABEL") val appLabel: String?,
    @SerializedName("ARQC") val arqc: String?
)
