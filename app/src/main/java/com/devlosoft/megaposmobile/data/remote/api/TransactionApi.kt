package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.AbortTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.AbortTransactionResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.AddMaterialRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.ChangeQuantityRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.ChangeQuantityResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.AddMaterialResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.CanRecoverTransactionResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.FinalizeTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.FinalizeTransactionResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.InvoiceDataDto
import com.devlosoft.megaposmobile.data.remote.dto.PackagingReconciliationDto
import com.devlosoft.megaposmobile.data.remote.dto.PauseTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.PauseTransactionResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.PrintTransactionResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.TodayTransactionDto
import com.devlosoft.megaposmobile.data.remote.dto.UpdatePackagingsRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.UpdateTransactionCustomerRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.UpdateTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.VoidItemRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApi {

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
        @Query("workstationId") workstationId: String,
        @Query("transactionId") transactionId: String? = null
    ): Response<CanRecoverTransactionResponseDto>

    @PUT("transaction/customer")
    suspend fun updateTransactionCustomer(
        @Body request: UpdateTransactionCustomerRequestDto
    ): Response<Unit>

    @PATCH("transaction/{transactionId}")
    suspend fun updateTransaction(
        @Path("transactionId") transactionId: String,
        @Body request: UpdateTransactionRequestDto
    ): Response<Unit>

    @GET("transaction/{transactionId}/details")
    suspend fun getTransactionDetails(
        @Path("transactionId") transactionId: String
    ): Response<InvoiceDataDto>

    @POST("transaction/pause")
    suspend fun pauseTransaction(
        @Body request: PauseTransactionRequestDto
    ): Response<PauseTransactionResponseDto>

    @POST("transaction/abort")
    suspend fun abortTransaction(
        @Body request: AbortTransactionRequestDto
    ): Response<AbortTransactionResponseDto>

    @PATCH("material/{transactionId}/void")
    suspend fun voidItem(
        @Path("transactionId") transactionId: String,
        @Body request: VoidItemRequestDto
    ): Response<Boolean>

    @PUT("material/{transactionId}/change-quantity")
    suspend fun changeQuantity(
        @Path("transactionId") transactionId: String,
        @Body request: ChangeQuantityRequestDto
    ): Response<ChangeQuantityResponseDto>

    @GET("material/{transactionId}/packaging-reconciliation")
    suspend fun getPackagingReconciliation(
        @Path("transactionId") transactionId: String
    ): Response<List<PackagingReconciliationDto>>

    @POST("material/{transactionId}/update-packagings")
    suspend fun updatePackagings(
        @Path("transactionId") transactionId: String,
        @Body request: UpdatePackagingsRequestDto
    ): Response<Boolean>

    @GET("transaction/today-completed")
    suspend fun getTodayCompletedTransactions(
        @Query("workstationId") workstationId: String
    ): Response<List<TodayTransactionDto>>
}
