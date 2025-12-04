package com.devlosoft.megaposmobile.domain.usecase

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.Token
import com.devlosoft.megaposmobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(code: String, password: String): Flow<Resource<Token>> {
        if (code.isBlank()) {
            return kotlinx.coroutines.flow.flow {
                emit(Resource.Error("El código de usuario es requerido"))
            }
        }
        if (password.isBlank()) {
            return kotlinx.coroutines.flow.flow {
                emit(Resource.Error("La contraseña es requerida"))
            }
        }
        return authRepository.login(code, password)
    }
}
