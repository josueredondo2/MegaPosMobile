package com.devlosoft.megaposmobile.data.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.api.SystemApi
import com.devlosoft.megaposmobile.domain.model.AppVersion
import com.devlosoft.megaposmobile.domain.repository.SystemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SystemRepositoryImpl @Inject constructor(
    private val systemApi: SystemApi
) : SystemRepository {

    override suspend fun getServerAppVersion(): Flow<Resource<AppVersion>> = flow {
        emit(Resource.Loading())
        try {
            val response = systemApi.getAppVersion()
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    emit(Resource.Success(dto.toDomain()))
                } ?: emit(Resource.Error("Respuesta vacía del servidor"))
            } else {
                emit(Resource.Error("Error: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido al obtener versión"))
        }
    }
}
