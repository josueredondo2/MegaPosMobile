package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.Token
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(code: String, password: String): Flow<Resource<Token>>
    suspend fun logout(): Flow<Resource<Unit>>
    fun isLoggedIn(): Flow<Boolean>

    /**
     * Verifica si la sesión actual del usuario sigue activa en el backend.
     * @return Flow con Resource<Boolean> indicando si la sesión está viva
     */
    suspend fun checkSessionStatus(): Flow<Resource<Boolean>>
}
