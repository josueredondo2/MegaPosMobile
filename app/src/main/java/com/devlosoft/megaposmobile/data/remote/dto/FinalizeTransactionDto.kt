package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FinalizeTransactionRequestDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("workstationId") val workstationId: String,
    @SerializedName("transactionId") val transactionId: String
)

data class FinalizeTransactionResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("transactionId") val transactionId: String?
)
