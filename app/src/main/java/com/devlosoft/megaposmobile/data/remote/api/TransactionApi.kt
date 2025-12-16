package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.AddMaterialRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.AddMaterialResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.CanRecoverTransactionResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.CreateTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.CreateTransactionResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.FinalizeTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.FinalizeTransactionResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.PrintTransactionResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApi {

    @POST("transaction")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequestDto
    ): Response<CreateTransactionResponseDto>

    @POST("material/add")
    suspend fun addMaterial(
        @Body request: AddMaterialRequestDto
    ): Response<AddMaterialResponseDto>

    @POST("transaction/finalize")
    suspend fun finalizeTransaction(
        @Body request: FinalizeTransactionRequestDto
    ): Response<FinalizeTransactionResponseDto>

    @GET("transaction/{transactionId}/print")
    suspend fun getPrintText(
        @Path("transactionId") transactionId: String,
        @Query("templateId") templateId: String = "01-FC",
        @Query("isReprint") isReprint: Boolean = false,
        @Query("copyNumber") copyNumber: Int = 0
    ): Response<PrintTransactionResponseDto>

    @GET("transaction/can-recover")
    suspend fun canRecoverTransaction(
        @Query("sessionId") sessionId: String,
        @Query("workstationId") workstationId: String
    ): Response<CanRecoverTransactionResponseDto>
}
