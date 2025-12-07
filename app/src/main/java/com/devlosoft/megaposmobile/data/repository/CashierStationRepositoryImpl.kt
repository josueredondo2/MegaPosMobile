package com.devlosoft.megaposmobile.data.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.api.CashierStationApi
import com.devlosoft.megaposmobile.data.remote.dto.ApiErrorDto
import com.devlosoft.megaposmobile.data.remote.dto.CloseStationRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.OpenStationRequestDto
import com.devlosoft.megaposmobile.domain.model.CloseStationResult
import com.devlosoft.megaposmobile.domain.model.OpenStationResult
import com.devlosoft.megaposmobile.domain.repository.CashierStationRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class CashierStationRepositoryImpl @Inject constructor(
    private val cashierStationApi: CashierStationApi,
    private val gson: Gson
) : CashierStationRepository {

    override suspend fun openStation(deviceId: String): Flow<Resource<OpenStationResult>> = flow {
        emit(Resource.Loading())

        try {
            val response = cashierStationApi.openStation(
                OpenStationRequestDto(macAddress = deviceId)
            )

            if (response.isSuccessful) {
                response.body()?.let { openStationResponse ->
                    emit(Resource.Success(openStationResponse.toDomain()))
                } ?: emit(Resource.Error("Respuesta vacía del servidor"))
            } else {
                val errorBody = response.errorBody()?.string()
                val apiError = try {
                    gson.fromJson(errorBody, ApiErrorDto::class.java)
                } catch (e: Exception) {
                    null
                }

                val errorMessage = apiError?.message ?: "Error al abrir la terminal"
                val errorCode = apiError?.errorCode

                emit(Resource.Error(errorMessage, errorCode))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Error de servidor: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.localizedMessage}"))
        }
    }

    override suspend fun closeStation(sessionId: String): Flow<Resource<CloseStationResult>> = flow {
        emit(Resource.Loading())

        try {
            val response = cashierStationApi.closeStation(
                CloseStationRequestDto(sessionId = sessionId)
            )

            if (response.isSuccessful) {
                response.body()?.let { closeStationResponse ->
                    emit(Resource.Success(closeStationResponse.toDomain()))
                } ?: emit(Resource.Error("Respuesta vacía del servidor"))
            } else {
                val errorBody = response.errorBody()?.string()
                val apiError = try {
                    gson.fromJson(errorBody, ApiErrorDto::class.java)
                } catch (e: Exception) {
                    null
                }

                val errorMessage = apiError?.message ?: "Error al cerrar la terminal"
                val errorCode = apiError?.errorCode

                emit(Resource.Error(errorMessage, errorCode))
            }
        } catch (e: HttpException) {
            emit(Resource.Error("Error de servidor: ${e.message()}"))
        } catch (e: IOException) {
            emit(Resource.Error("Error de conexión. Verifique su conexión a internet."))
        } catch (e: Exception) {
            emit(Resource.Error("Error inesperado: ${e.localizedMessage}"))
        }
    }
}
