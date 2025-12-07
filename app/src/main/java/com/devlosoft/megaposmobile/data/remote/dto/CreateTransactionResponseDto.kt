package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateTransactionResponseDto(
    @SerializedName("transactionCode")
    val transactionCode: String
)
