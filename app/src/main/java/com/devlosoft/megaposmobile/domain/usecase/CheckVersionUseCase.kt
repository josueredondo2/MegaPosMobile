package com.devlosoft.megaposmobile.domain.usecase

import com.devlosoft.megaposmobile.BuildConfig
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.repository.SystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class VersionCheckResult(
    val isValid: Boolean,
    val localVersion: String,
    val serverVersion: String?,
    val errorMessage: String? = null
)

class CheckVersionUseCase @Inject constructor(
    private val systemRepository: SystemRepository
) {
    suspend operator fun invoke(): Flow<Resource<VersionCheckResult>> = flow {
        emit(Resource.Loading())

        val localVersion = BuildConfig.VERSION_NAME

        systemRepository.getServerAppVersion().collect { result ->
            when (result) {
                is Resource.Success -> {
                    val serverVersion = result.data?.version ?: ""
                    val isValid = localVersion == serverVersion
                    emit(Resource.Success(VersionCheckResult(
                        isValid = isValid,
                        localVersion = localVersion,
                        serverVersion = serverVersion,
                        errorMessage = if (!isValid)
                            "Versión de la app ($localVersion) no coincide con el servidor ($serverVersion). Por favor actualice la aplicación."
                        else null
                    )))
                }
                is Resource.Error -> {
                    emit(Resource.Error(result.message ?: "Error al verificar versión"))
                }
                is Resource.Loading -> { /* Ignorar */ }
            }
        }
    }
}
