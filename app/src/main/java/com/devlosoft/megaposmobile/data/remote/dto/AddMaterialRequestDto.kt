package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MaterialDetailsDto(
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
    val authorizedBy: String? = null
)

data class TransactionDetailsDto(
    @SerializedName("customerId")
    val customerId: String? = null,

    @SerializedName("customerIdType")
    val customerIdType: String? = null,

    @SerializedName("customerName")
    val customerName: String? = null,

    @SerializedName("economicActivityId")
    val economicActivityId: String? = null,

    @SerializedName("transactionTypeCode")
    val transactionTypeCode: String? = "CO"
)

data class AddMaterialRequestDto(
    @SerializedName("transactionId")
    val transactionId: String = "",

    @SerializedName("sessionId")
    val sessionId: String? = null,

    @SerializedName("workstationId")
    val workstationId: String? = null,

    @SerializedName("material")
    val material: MaterialDetailsDto,

    @SerializedName("transaction")
    val transaction: TransactionDetailsDto
)
