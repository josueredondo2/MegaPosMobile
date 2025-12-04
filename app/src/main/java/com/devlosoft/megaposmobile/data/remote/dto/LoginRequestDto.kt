package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("code")
    val code: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("attempts")
    val attempts: Int = 0
)
