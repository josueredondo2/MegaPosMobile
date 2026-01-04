package com.devlosoft.megaposmobile.data.repository

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.api.CustomerApi
import com.devlosoft.megaposmobile.data.remote.api.FelApi
import com.devlosoft.megaposmobile.data.remote.api.TransactionApi
import com.devlosoft.megaposmobile.data.remote.dto.AddMaterialRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.ChangeQuantityRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.DataphoneDataDto
import com.devlosoft.megaposmobile.data.remote.dto.ErrorResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.AbortTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.FinalizeTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.PackagingItemDto
import com.devlosoft.megaposmobile.data.remote.dto.PauseTransactionRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.UpdatePackagingsRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.UpdateTransactionCustomerRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.ValidateClientRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.ValidateClientResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.VoidItemRequestDto
import com.devlosoft.megaposmobile.data.local.dao.ActiveTransactionDao
import com.devlosoft.megaposmobile.data.local.entity.ActiveTransactionEntity
import com.devlosoft.megaposmobile.domain.model.AddMaterialResult
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.PackagingItem
import com.devlosoft.megaposmobile.domain.model.PrintDocument
import com.devlosoft.megaposmobile.domain.model.TodayTransaction
import com.devlosoft.megaposmobile.domain.model.TransactionRecoveryResult
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import javax.inject.Inject

class BillingRepositoryImpl @Inject constructor(
    private val customerApi: CustomerApi,
    private val transactionApi: TransactionApi,
    private val felApi: FelApi,
    private val activeTransactionDao: ActiveTransactionDao
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

    override suspend fun addMaterial(
        transactionId: String,
        itemPosId: String,
        quantity: Double,
        partyAffiliationTypeCode: String?,
        sessionId: String?,
        workstationId: String?,
        customerId: String?,
        customerIdType: String?,
        customerName: String?,
        isAuthorized: Boolean,
        authorizedBy: String?
    ): Flow<Resource<AddMaterialResult>> = flow {
        emit(Resource.Loading())
        try {
            val request = AddMaterialRequestDto(
                transactionId = transactionId,
                itemPosId = itemPosId,
                quantity = quantity,
                partyAffiliationTypeCode = partyAffiliationTypeCode,
                sessionId = sessionId,
                workstationId = workstationId,
                customerId = customerId,
                customerIdType = customerIdType,
                customerName = customerName,
                isAuthorized = isAuthorized,
                authorizedBy = authorizedBy
            )
            val response = transactionApi.addMaterial(request)
            if (response.isSuccessful) {
                val body = response.body()
                val invoiceData = body?.invoiceData?.toDomain() ?: InvoiceData()
                val result = AddMaterialResult(
                    transactionId = body?.transactionId,
                    invoiceData = invoiceData
                )
                emit(Resource.Success(result))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorResponse = ErrorResponseDto.fromJson(errorBody)
                val errorMessage = ErrorResponseDto.getSpanishMessage(errorResponse?.errorCode)
                emit(Resource.Error(errorMessage, errorCode = errorResponse?.errorCode))
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
        transactionId: String,
        dataphoneData: DataphoneDataDto?
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val request = FinalizeTransactionRequestDto(
                sessionId = sessionId,
                workstationId = workstationId,
                transactionId = transactionId,
                dataphoneData = dataphoneData
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
                val errorMessage = errorResponse?.message
                    ?: ErrorResponseDto.getSpanishMessage(errorResponse?.errorCode)
                emit(Resource.Error(errorMessage))
            }
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexion. Verifique su conexion a internet."))
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

    override suspend fun canRecoverTransaction(
        sessionId: String,
        workstationId: String,
        transactionId: String?
    ): Flow<Resource<TransactionRecoveryResult>> = flow {
        emit(Resource.Loading())
        try {
            val response = transactionApi.canRecoverTransaction(
                sessionId = sessionId,
                workstationId = workstationId,
                transactionId = transactionId
            )
            if (response.isSuccessful) {
                val result = response.body()?.toDomain()
                if (result != null) {
                    emit(Resource.Success(result))
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

    override suspend fun updateTransactionCustomer(
        transactionId: String,
        sessionId: String,
        workstationId: String,
        customerId: Int,
        customerIdType: String,
        customerName: String,
        affiliateType: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val request = UpdateTransactionCustomerRequestDto(
                transactionId = transactionId,
                sessionId = sessionId,
                workstationId = workstationId,
                customerId = customerId,
                customerIdType = customerIdType,
                customerName = customerName,
                affiliateType = affiliateType
            )
            val response = transactionApi.updateTransactionCustomer(request)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
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

    override suspend fun getTransactionDetails(
        transactionId: String
    ): Flow<Resource<InvoiceData>> = flow {
        emit(Resource.Loading())
        try {
            val response = transactionApi.getTransactionDetails(transactionId)
            if (response.isSuccessful) {
                val invoiceData = response.body()?.toDomain() ?: InvoiceData()
                emit(Resource.Success(invoiceData))
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

    override suspend fun pauseTransaction(
        transactionId: String,
        sessionId: String,
        workstationId: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val request = PauseTransactionRequestDto(
                transactionId = transactionId,
                sessionId = sessionId,
                workstationId = workstationId
            )
            val response = transactionApi.pauseTransaction(request)
            if (response.isSuccessful) {
                val success = response.body()?.success ?: false
                if (success) {
                    emit(Resource.Success(true))
                } else {
                    val message = response.body()?.message ?: "Error al pausar transacción"
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

    override suspend fun abortTransaction(
        sessionId: String,
        workstationId: String,
        transactionId: String,
        reason: String,
        authorizingOperator: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val request = AbortTransactionRequestDto(
                sessionId = sessionId,
                workstationId = workstationId,
                transactionId = transactionId,
                reason = reason,
                authorizingOperator = authorizingOperator
            )
            val response = transactionApi.abortTransaction(request)
            if (response.isSuccessful) {
                val success = response.body()?.success ?: false
                if (success) {
                    emit(Resource.Success(true))
                } else {
                    val message = response.body()?.message ?: "Error al abortar transacción"
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

    override suspend fun voidItem(
        transactionId: String,
        itemPosId: String,
        authorizedOperator: String,
        affiliateType: String,
        deleteAll: Boolean
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val request = VoidItemRequestDto(
                itemPosId = itemPosId,
                authorizedOperator = authorizedOperator,
                affiliateType = affiliateType,
                deleteAll = deleteAll
            )
            val response = transactionApi.voidItem(transactionId, request)
            if (response.isSuccessful) {
                val success = response.body() ?: false
                if (success) {
                    emit(Resource.Success(true))
                } else {
                    emit(Resource.Error("Error al eliminar artículo"))
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

    override suspend fun changeQuantity(
        transactionId: String,
        itemPosId: String,
        lineNumber: Int,
        newQuantity: Double,
        partyAffiliationTypeCode: String,
        isAuthorized: Boolean,
        authorizedBy: String?
    ): Flow<Resource<InvoiceData>> = flow {
        emit(Resource.Loading())
        try {
            val request = ChangeQuantityRequestDto(
                itemPosId = itemPosId,
                lineNumber = lineNumber,
                newQuantity = newQuantity,
                partyAffiliationTypeCode = partyAffiliationTypeCode,
                isAuthorized = isAuthorized,
                authorizedBy = authorizedBy
            )
            val response = transactionApi.changeQuantity(transactionId, request)
            if (response.isSuccessful) {
                val invoiceData = response.body()?.toDomain() ?: InvoiceData()
                emit(Resource.Success(invoiceData))
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

    // Active transaction persistence methods
    override suspend fun saveActiveTransactionId(transactionId: String) {
        activeTransactionDao.saveActiveTransaction(
            ActiveTransactionEntity(transactionId = transactionId)
        )
    }

    override suspend fun getActiveTransactionId(): String? {
        return activeTransactionDao.getActiveTransaction()?.transactionId
    }

    override suspend fun clearActiveTransactionId() {
        activeTransactionDao.clearActiveTransaction()
    }

    // Packaging methods
    override suspend fun getPackagingReconciliation(
        transactionId: String
    ): Flow<Resource<List<PackagingItem>>> = flow {
        emit(Resource.Loading())
        try {
            val response = transactionApi.getPackagingReconciliation(transactionId)
            if (response.isSuccessful) {
                val rawItems = response.body()
                android.util.Log.d("PackagingDebug", "Raw response: $rawItems")
                rawItems?.forEach { item ->
                    android.util.Log.d("PackagingDebug", "Item: itemPosId=${item.itemPosId}, desc=${item.description}, pending=${item.quantityPending}, redeemed=${item.quantityRedeemed}")
                }
                val packagingItems = rawItems?.map { it.toDomain() } ?: emptyList()
                emit(Resource.Success(packagingItems))
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

    override suspend fun updatePackagings(
        transactionId: String,
        packagings: List<PackagingItemDto>,
        affiliateType: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val request = UpdatePackagingsRequestDto(
                packagings = packagings,
                affiliateType = affiliateType
            )
            val response = transactionApi.updatePackagings(transactionId, request)
            if (response.isSuccessful) {
                val success = response.body() ?: false
                if (success) {
                    emit(Resource.Success(true))
                } else {
                    emit(Resource.Error("Error al actualizar envases"))
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

    // Today's completed transactions
    override suspend fun getTodayCompletedTransactions(workstationId: String): Flow<Resource<List<TodayTransaction>>> = flow {
        emit(Resource.Loading())
        try {
            val response = transactionApi.getTodayCompletedTransactions(workstationId)
            if (response.isSuccessful) {
                val transactions = response.body()?.map { it.toDomain() } ?: emptyList()
                emit(Resource.Success(transactions))
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

    // FEL client validation
    override suspend fun validateClientForFel(
        identificationType: String,
        identification: String,
        partyId: Int,
        documentType: String,
        userLogin: String
    ): Flow<Resource<ValidateClientResponseDto>> = flow {
        emit(Resource.Loading())
        try {
            val request = ValidateClientRequestDto(
                identificationType = identificationType,
                identification = identification,
                partyId = partyId,
                documentType = documentType,
                userLogin = userLogin
            )
            Log.d("BillingRepo", "FEL Validate Request: $request")
            val response = felApi.validateClient(request)
            Log.d("BillingRepo", "FEL Validate Response code: ${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("BillingRepo", "FEL Validate Body: result=${body?.result}, stackTrace=${body?.stackTrace}")
                if (body != null) {
                    emit(Resource.Success(body))
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
}
