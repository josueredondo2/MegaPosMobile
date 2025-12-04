package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.Token
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(code: String, password: String): Flow<Resource<Token>>
    suspend fun logout(): Flow<Resource<Unit>>
    fun isLoggedIn(): Flow<Boolean>
}
