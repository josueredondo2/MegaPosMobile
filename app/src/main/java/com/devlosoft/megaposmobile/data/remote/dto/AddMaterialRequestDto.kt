package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AddMaterialRequestDto(
    @SerializedName("transactionId")
    val transactionId: String = "",

    @SerializedName("itemPosId")
    val itemPosId: String,

    @SerializedName("quantity")
    val quantity: Double = 1.0,

    @SerializedName("partyAffiliationTypeCode")
    val partyAffiliationTypeCode: String? = null,

    @SerializedName("price")
    val price: Double? = null,

    @SerializedName("isAuthorized")
    val isAuthorized: Boolean = false,

    @SerializedName("authorizedBy")
    val authorizedBy: String? = null,

    @SerializedName("sessionId")
    val sessionId: String? = null,

    @SerializedName("workstationId")
    val workstationId: String? = null
)
