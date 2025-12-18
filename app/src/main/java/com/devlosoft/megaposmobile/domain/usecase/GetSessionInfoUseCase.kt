package com.devlosoft.megaposmobile.domain.usecase

import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for retrieving current session information.
 * Centralizes the session validation logic that was duplicated across ViewModels.
 */
class GetSessionInfoUseCase @Inject constructor(
    private val sessionManager: SessionManager
) {
    /**
     * Holds the session and station identifiers
     */
    data class SessionInfo(
        val sessionId: String,
        val stationId: String
    )

    /**
     * Retrieves the current session information.
     *
     * @return Result.success with SessionInfo if session is valid, Result.failure otherwise
     */
    suspend operator fun invoke(): Result<SessionInfo> {
        return try {
            val sessionId = sessionManager.getSessionId().first()
            val stationId = sessionManager.getStationId().first()

            if (sessionId.isNullOrBlank() || stationId.isNullOrBlank()) {
                Result.failure(SessionException("No hay sesión activa"))
            } else {
                Result.success(SessionInfo(sessionId, stationId))
            }
        } catch (e: Exception) {
            Result.failure(SessionException("Error al obtener información de sesión: ${e.message}"))
        }
    }

    /**
     * Checks if there is an active session without throwing exceptions.
     *
     * @return true if session is valid, false otherwise
     */
    suspend fun hasActiveSession(): Boolean {
        return invoke().isSuccess
    }
}

/**
 * Exception thrown when session is invalid or not found
 */
class SessionException(message: String) : Exception(message)
