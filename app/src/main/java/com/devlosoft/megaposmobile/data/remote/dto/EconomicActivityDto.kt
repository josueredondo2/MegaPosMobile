package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EconomicActivityDto(
    @SerializedName("status")
    val status: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("description")
    val description: String
)
