package com.devlosoft.megaposmobile.data.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.data.remote.api.AuthApi
import com.devlosoft.megaposmobile.data.remote.dto.ApiErrorDto
import com.devlosoft.megaposmobile.data.remote.dto.LoginRequestDto
import com.devlosoft.megaposmobile.domain.model.Token
import com.devlosoft.megaposmobile.domain.repository.AuthRepository
import com.devlosoft.megaposmobile.util.CryptoHelper
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : AuthRepository {

    override suspend fun login(code: String, password: String): Flow<Resource<Token>> = flow {
        emit(Resource.Loading())

        try {
            // Encrypt password before sending to server
            val encryptedPassword = CryptoHelper.encrypt(password)
            val response = authApi.login(LoginRequestDto(code = code, password = encryptedPassword))

            if (response.isSuccessful) {
                response.body()?.let { loginResponse ->
                    val token = loginResponse.toDomain()
                    sessionManager.saveSession(
                        accessToken = token.accessToken,
                        userCode = code
                    )
                    emit(Resource.Success(token))
                } ?: emit(Resource.Error("Respuesta vacía del servidor"))
            } else {
                val errorBody = response.errorBody()?.string()
                val apiError = try {
                    gson.fromJson(errorBody, ApiErrorDto::class.java)
                } catch (e: Exception) {
                    null
                }

                val errorMessage = apiError?.message ?: "Error de autenticación"
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

    override suspend fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        try {
            val response = authApi.logout()

            if (response.isSuccessful) {
                sessionManager.clearSession()
                emit(Resource.Success(Unit))
            } else {
                // Even if logout fails on server, clear local session
                sessionManager.clearSession()
                emit(Resource.Success(Unit))
            }
        } catch (e: Exception) {
            // Clear session even on error
            sessionManager.clearSession()
            emit(Resource.Success(Unit))
        }
    }

    override fun isLoggedIn(): Flow<Boolean> = sessionManager.isLoggedIn()
}
