package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateTransactionRequestDto(
    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("workstationId")
    val workstationId: String,

    @SerializedName("customerId")
    val customerId: String? = null,

    @SerializedName("customerIdType")
    val customerIdType: String? = null,

    @SerializedName("customerName")
    val customerName: String? = null
)
