package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ValidateClientRequestDto(
    @SerializedName("identificationType")
    val identificationType: String,

    @SerializedName("identification")
    val identification: String,

    @SerializedName("partyId")
    val partyId: Int,

    @SerializedName("documentType")
    val documentType: String,

    @SerializedName("userLogin")
    val userLogin: String
)
