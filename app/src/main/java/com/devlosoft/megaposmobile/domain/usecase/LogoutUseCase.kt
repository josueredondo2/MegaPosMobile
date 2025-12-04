package com.devlosoft.megaposmobile.domain.usecase

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Flow<Resource<Unit>> {
        return authRepository.logout()
    }
}
