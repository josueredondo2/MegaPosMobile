package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateTransactionCustomerRequestDto(
    @SerializedName("transactionId")
    val transactionId: String,

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("workstationId")
    val workstationId: String,

    @SerializedName("customerId")
    val customerId: Int,

    @SerializedName("customerIdType")
    val customerIdType: String,

    @SerializedName("customerName")
    val customerName: String,

    @SerializedName("affiliateType")
    val affiliateType: String
)
