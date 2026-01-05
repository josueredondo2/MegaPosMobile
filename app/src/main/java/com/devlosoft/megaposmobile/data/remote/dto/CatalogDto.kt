package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CatalogTypeDto(
    @SerializedName("catalogTypeId")
    val catalogTypeId: Int,

    @SerializedName("catalogName")
    val catalogName: String
)

data class CatalogItemDto(
    @SerializedName("unitSalesType")
    val unitSalesType: Int?,

    @SerializedName("catalogItemName")
    val catalogItemName: String,

    @SerializedName("catalogItemImage")
    val catalogItemImage: String?,

    @SerializedName("itemPosId")
    val itemPosId: String
)
