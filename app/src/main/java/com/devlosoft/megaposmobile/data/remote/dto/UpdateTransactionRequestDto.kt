package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UpdateTransactionRequestDto(
    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("workstationId")
    val workstationId: String,

    @SerializedName("customerId")
    val customerId: Int? = null,

    @SerializedName("customerIdType")
    val customerIdType: String? = null,

    @SerializedName("customerName")
    val customerName: String? = null,

    @SerializedName("affiliateType")
    val affiliateType: String? = null,

    @SerializedName("transactionTypeCode")
    val transactionTypeCode: String? = null
)
