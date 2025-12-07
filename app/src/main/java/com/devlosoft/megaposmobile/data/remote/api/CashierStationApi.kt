package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.CloseStationRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.CloseStationResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.OpenStationRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.OpenStationResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface CashierStationApi {

    @POST("cashier-station/open")
    suspend fun openStation(@Body request: OpenStationRequestDto): Response<OpenStationResponseDto>

    @POST("cashier-station/close")
    suspend fun closeStation(@Body request: CloseStationRequestDto): Response<CloseStationResponseDto>
}
