package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CloseStationRequestDto(
    @SerializedName("sessionId")
    val sessionId: String
)
