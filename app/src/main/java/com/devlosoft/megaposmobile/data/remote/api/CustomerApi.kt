package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.CustomerDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CustomerApi {

    @GET("customer/{identification}")
    suspend fun searchCustomer(
        @Path("identification") identification: String
    ): Response<List<CustomerDto>>
}
