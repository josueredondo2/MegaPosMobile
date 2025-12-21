package com.devlosoft.megaposmobile.domain.usecase

import com.devlosoft.megaposmobile.data.remote.api.AuthApi
import com.devlosoft.megaposmobile.data.remote.dto.GrantProcessExecRequestDto
import com.devlosoft.megaposmobile.util.TablaDesencriptado
import javax.inject.Inject

/**
 * Use case for authorizing a process execution.
 * Used when a user doesn't have direct access to a process and needs
 * authorization from another user.
 */
class AuthorizeProcessUseCase @Inject constructor(
    private val authApi: AuthApi
) {
    /**
     * Attempts to authorize a process execution using another user's credentials.
     *
     * @param userCode The authorizing user's code
     * @param password The authorizing user's password (will be encrypted)
     * @param processCode The process code to authorize
     * @return Result.success(true) if authorized, Result.failure with error message otherwise
     */
    suspend operator fun invoke(
        userCode: String,
        password: String,
        processCode: String
    ): Result<Boolean> {
        return try {
            // Encrypt password using the same method as login
            val encryptedPassword = TablaDesencriptado.encrypt(password)

            val request = GrantProcessExecRequestDto(
                userCode = userCode,
                userPassword = encryptedPassword,
                processCode = processCode
            )

            val response = authApi.grantProcessExec(request)

            if (response.isSuccessful && response.body() == true) {
                Result.success(true)
            } else {
                Result.failure(AuthorizationException("Credenciales inválidas o sin permisos"))
            }
        } catch (e: Exception) {
            Result.failure(AuthorizationException("Error de conexión: ${e.message}"))
        }
    }
}

/**
 * Exception thrown when authorization fails
 */
class AuthorizationException(message: String) : Exception(message)
