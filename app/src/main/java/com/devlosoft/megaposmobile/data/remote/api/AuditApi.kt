package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.AuditLogRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API para operaciones de auditoría/bitácora.
 */
interface AuditApi {

    /**
     * Registra un evento de auditoría en el backend.
     */
    @POST("audit/log")
    suspend fun log(
        @Body request: AuditLogRequestDto
    ): Response<Unit>
}
