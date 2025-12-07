package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.AddMaterialRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.AddMaterialResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.CreateTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.CreateTransactionResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TransactionApi {

    @POST("transaction")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequestDto
    ): Response<CreateTransactionResponseDto>

    @POST("material/add")
    suspend fun addMaterial(
        @Body request: AddMaterialRequestDto
    ): Response<AddMaterialResponseDto>
}
