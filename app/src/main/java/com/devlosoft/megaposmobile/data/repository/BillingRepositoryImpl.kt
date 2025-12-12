package com.devlosoft.megaposmobile.data.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.api.CustomerApi
import com.devlosoft.megaposmobile.data.remote.api.TransactionApi
import com.devlosoft.megaposmobile.data.remote.dto.AddMaterialRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.CreateTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.ErrorResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.FinalizeTransactionRequestDto
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.PrintDocument
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class BillingRepositoryImpl @Inject constructor(
    private val customerApi: CustomerApi,
    private val transactionApi: TransactionApi
) : BillingRepository {

    override suspend fun searchCustomer(identification: String): Flow<Resource<List<Customer>>> = flow {
        emit(Resource.Loading())
        try {
            val response = customerApi.searchCustomer(identification)
            if (response.isSuccessful) {
                val customers = response.body()?.map { it.toDomain() } ?: emptyList()
                emit(Resource.Success(customers))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = ErrorResponseDto.fromJson(errorBody)
                val errorMessage = ErrorResponseDto.getSpanishMessage(errorResponse?.errorCode)
                emit(Resource.Error(errorMessage))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.message}"))
        }
    }

    override suspend fun createTransaction(
        sessionId: String,
        workstationId: String,
        customerId: String?,
        customerIdType: String?,
        customerName: String?
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val request = CreateTransactionRequestDto(
                sessionId = sessionId,
                workstationId = workstationId,
                customerId = customerId,
                customerIdType = customerIdType,
                customerName = customerName
            )
            val response = transactionApi.createTransaction(request)
            if (response.isSuccessful) {
                val transactionCode = response.body()?.transactionCode
                if (transactionCode != null) {
                    emit(Resource.Success(transactionCode))
                } else {
                    emit(Resource.Error("Respuesta vacía del servidor"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = ErrorResponseDto.fromJson(errorBody)
                val errorMessage = ErrorResponseDto.getSpanishMessage(errorResponse?.errorCode)
                emit(Resource.Error(errorMessage))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.message}"))
        }
    }

    override suspend fun addMaterial(
        transactionId: String,
        itemPosId: String,
        quantity: Double,
        partyAffiliationTypeCode: String?
    ): Flow<Resource<InvoiceData>> = flow {
        emit(Resource.Loading())
        try {
            val request = AddMaterialRequestDto(
                transactionId = transactionId,
                itemPosId = itemPosId,
                quantity = quantity,
                partyAffiliationTypeCode = partyAffiliationTypeCode
            )
            val response = transactionApi.addMaterial(request)
            if (response.isSuccessful) {
                val invoiceData = response.body()?.invoiceData?.toDomain()
                if (invoiceData != null) {
                    emit(Resource.Success(invoiceData))
                } else {
                    emit(Resource.Error("Respuesta vacía del servidor"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = ErrorResponseDto.fromJson(errorBody)
                val errorMessage = ErrorResponseDto.getSpanishMessage(errorResponse?.errorCode)
                emit(Resource.Error(errorMessage))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.message}"))
        }
    }

    override suspend fun finalizeTransaction(
        sessionId: String,
        workstationId: String,
        transactionId: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val request = FinalizeTransactionRequestDto(
                sessionId = sessionId,
                workstationId = workstationId,
                transactionId = transactionId
            )
            val response = transactionApi.finalizeTransaction(request)
            if (response.isSuccessful) {
                val success = response.body()?.success ?: false
                if (success) {
                    emit(Resource.Success(true))
                } else {
                    val message = response.body()?.message ?: "Error al finalizar transacción"
                    emit(Resource.Error(message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = ErrorResponseDto.fromJson(errorBody)
                val errorMessage = ErrorResponseDto.getSpanishMessage(errorResponse?.errorCode)
                emit(Resource.Error(errorMessage))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.message}"))
        }
    }

    override suspend fun getPrintDocuments(
        transactionId: String,
        templateId: String,
        isReprint: Boolean,
        copyNumber: Int
    ): Flow<Resource<List<PrintDocument>>> = flow {
        emit(Resource.Loading())
        try {
            val response = transactionApi.getPrintText(
                transactionId = transactionId,
                templateId = templateId,
                isReprint = isReprint,
                copyNumber = copyNumber
            )
            if (response.isSuccessful) {
                val documents = response.body()?.documents?.map { dto ->
                    PrintDocument(
                        documentType = dto.documentType,
                        printText = dto.printText,
                        couponNumber = dto.couponNumber,
                        promotionName = dto.promotionName
                    )
                } ?: emptyList()
                emit(Resource.Success(documents))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = ErrorResponseDto.fromJson(errorBody)
                val errorMessage = ErrorResponseDto.getSpanishMessage(errorResponse?.errorCode)
                emit(Resource.Error(errorMessage))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.message}"))
        }
    }
}
