package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PauseTransactionRequestDto(
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("workstationId") val workstationId: String
)

data class PauseTransactionResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)
