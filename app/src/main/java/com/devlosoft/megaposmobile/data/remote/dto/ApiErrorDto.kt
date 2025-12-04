package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiErrorDto(
    @SerializedName("errorCode")
    val errorCode: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("statusCode")
    val statusCode: Int
)
