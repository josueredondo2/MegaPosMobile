package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.CloseStationResult
import com.google.gson.annotations.SerializedName

data class CloseStationResponseDto(
    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("stationId")
    val stationId: String,

    @SerializedName("success")
    val success: Boolean
) {
    fun toDomain(): CloseStationResult = CloseStationResult(
        sessionId = sessionId,
        stationId = stationId,
        success = success
    )
}
