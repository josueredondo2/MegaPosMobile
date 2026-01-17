package com.devlosoft.megaposmobile.data.repository

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.dataphone.DataphoneManager
import com.devlosoft.megaposmobile.core.state.DataphoneState
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.remote.api.PaymentApi
import com.devlosoft.megaposmobile.data.remote.dto.ApiErrorDto
import com.devlosoft.megaposmobile.data.remote.dto.CloseDataphoneRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.CloseDataphoneResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.DataphoneDataDto
import com.devlosoft.megaposmobile.data.remote.dto.PaxCloseResponseDto
import com.devlosoft.megaposmobile.domain.repository.AuditRepository
import com.devlosoft.megaposmobile.domain.repository.PaymentRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentApi: PaymentApi,
    private val gson: Gson,
    private val dataphoneManager: DataphoneManager,
    private val dataphoneState: DataphoneState,
    private val serverConfigDao: ServerConfigDao,
    private val auditRepository: AuditRepository
) : PaymentRepository {

    companion object {
        private const val TAG = "PaymentRepositoryImpl"
    }

    override suspend fun processDataphonePayment(
        transactionId: String,
        amount: Double
    ): Flow<Resource<DataphoneDataDto>> = flow {
        emit(Resource.Loading())

        try {
            // Format amount as currency for user message
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR")).apply {
                maximumFractionDigits = 0
            }
            val formattedAmount = numberFormat.format(amount)

            // Log audit before calling dataphone
            auditRepository.log(
                action = "ADD",
                detail = "Iniciando pago datáfono por $formattedAmount - Trans: $transactionId",
                transactionId = transactionId
            )

            // Call dataphone for real payment
            val paymentResult = dataphoneManager.processPayment(amount.toLong())

            paymentResult.fold(
                onSuccess = { dataphoneResult ->
                    Log.d(TAG, "Payment successful: auth=${dataphoneResult.authorizationCode}")

                    // Update terminal ID if it's new or different
                    val newTerminalId = dataphoneResult.terminalid ?: ""
                    if (newTerminalId.isNotBlank()) {
                        val currentTerminalId = dataphoneState.getTerminalId()
                        if (currentTerminalId.isBlank() || currentTerminalId != newTerminalId) {
                            Log.d(TAG, "Terminal ID changed: '$currentTerminalId' -> '$newTerminalId'")
                            serverConfigDao.updateDataphoneTerminalId(newTerminalId)
                            dataphoneState.setTerminalId(newTerminalId)
                        }
                    }

                    // Create DTO with dataphone data
                    val dataphoneData = DataphoneDataDto(
                        authorizationCode = dataphoneResult.authorizationCode,
                        panmasked = dataphoneResult.panmasked?.replace("*", ""),
                        cardholder = dataphoneResult.cardholder,
                        terminalid = dataphoneResult.terminalid ?: "",
                        receiptNumber = dataphoneResult.receiptNumber,
                        rrn = dataphoneResult.rrn,
                        stan = dataphoneResult.stan,
                        ticket = dataphoneResult.ticket,
                        totalAmount = dataphoneResult.totalAmount
                    )

                    emit(Resource.Success(dataphoneData))
                },
                onFailure = { error ->
                    Log.e(TAG, "Payment failed", error)
                    emit(Resource.Error(
                        error.message ?: "Error al procesar pago en datáfono"
                    ))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during payment processing", e)
            emit(Resource.Error("Error inesperado: ${e.localizedMessage}"))
        }
    }

    override suspend fun closeDataphone(
        pointOfSaleCode: String,
        terminalId: String?,
        paxResponse: PaxCloseResponseDto?
    ): Flow<Resource<CloseDataphoneResponseDto>> = flow {
        emit(Resource.Loading())

        try {
            val request = CloseDataphoneRequestDto(
                acquirerCode = "BAC",
                pointOfSaleCode = pointOfSaleCode,
                terminalId = terminalId,
                paxResponse = paxResponse
            )

            val response = paymentApi.closeDataphone(request)

            if (response.isSuccessful) {
                response.body()?.let { closeResponse ->
                    if (closeResponse.success) {
                        emit(Resource.Success(closeResponse))
                    } else {
                        emit(Resource.Error(closeResponse.message ?: "Error al procesar cierre"))
                    }
                } ?: emit(Resource.Error("Respuesta vacia del servidor"))
            } else {
                val errorBody = response.errorBody()?.string()
                val apiError = try {
                    gson.fromJson(errorBody, ApiErrorDto::class.java)
                } catch (e: Exception) {
                    null
                }

                val errorMessage = apiError?.message ?: "Error al enviar cierre de datafono"
                val errorCode = apiError?.errorCode

                emit(Resource.Error(errorMessage, errorCode))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Error de servidor: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexion. Verifique su conexion a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.localizedMessage}"))
        }
    }
}
