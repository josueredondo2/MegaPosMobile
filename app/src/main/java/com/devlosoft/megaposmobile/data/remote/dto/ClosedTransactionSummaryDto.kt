package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para el resumen de una transaccion cerrada en el cierre de datafono.
 * Contiene la informacion minima para identificar cada transaccion.
 */
data class ClosedTransactionSummaryDto(
    @SerializedName("transactionId")
    val transactionId: String,

    @SerializedName("authorizationId")
    val authorizationId: String?,

    @SerializedName("cardNumber")
    val cardNumber: String?,

    @SerializedName("amount")
    val amount: Double = 0.0
)
