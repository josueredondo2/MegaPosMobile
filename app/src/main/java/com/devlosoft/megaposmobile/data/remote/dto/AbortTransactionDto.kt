package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AbortTransactionRequestDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("workstationId") val workstationId: String,
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("authorizingOperator") val authorizingOperator: String
)

data class AbortTransactionResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)
