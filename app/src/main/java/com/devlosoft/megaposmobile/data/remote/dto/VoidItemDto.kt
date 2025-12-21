package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VoidItemRequestDto(
    @SerializedName("itemPosId") val itemPosId: String,
    @SerializedName("authorizedOperator") val authorizedOperator: String,
    @SerializedName("affiliateType") val affiliateType: String,
    @SerializedName("deleteAll") val deleteAll: Boolean
)

data class VoidItemResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)
