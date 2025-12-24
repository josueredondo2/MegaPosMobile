package com.devlosoft.megaposmobile.data.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.api.PaymentApi
import com.devlosoft.megaposmobile.data.remote.dto.ApiErrorDto
import com.devlosoft.megaposmobile.data.remote.dto.CloseDataphoneRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.CloseDataphoneResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.PaxCloseResponseDto
import com.devlosoft.megaposmobile.domain.repository.PaymentRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val paymentApi: PaymentApi,
    private val gson: Gson
) : PaymentRepository {

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
