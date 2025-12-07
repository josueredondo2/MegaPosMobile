package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.OpenStationResult
import com.google.gson.annotations.SerializedName

data class OpenStationResponseDto(
    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("stationId")
    val stationId: String,

    @SerializedName("isNewSession")
    val isNewSession: Boolean
) {
    fun toDomain(): OpenStationResult = OpenStationResult(
        sessionId = sessionId,
        stationId = stationId,
        isNewSession = isNewSession
    )
}
