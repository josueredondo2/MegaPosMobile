package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GrantProcessExecRequestDto(
    @SerializedName("UserCode")
    val userCode: String,
    @SerializedName("UserPassword")
    val userPassword: String,
    @SerializedName("ProcessCode")
    val processCode: String
)
