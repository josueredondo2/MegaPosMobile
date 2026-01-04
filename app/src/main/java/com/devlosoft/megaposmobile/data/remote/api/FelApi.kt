package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.EconomicActivitySearchResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.ValidateClientRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.ValidateClientResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FelApi {
    @POST("client/validate")
    suspend fun validateClient(
        @Body request: ValidateClientRequestDto
    ): Response<ValidateClientResponseDto>

    @GET("economic-activities/search")
    suspend fun searchEconomicActivities(
        @Query("userLogin") userLogin: String,
        @Query("searchTerm") searchTerm: String,
        @Query("pageSize") pageSize: Int,
        @Query("pageNumber") pageNumber: Int
    ): Response<EconomicActivitySearchResponseDto>
}
