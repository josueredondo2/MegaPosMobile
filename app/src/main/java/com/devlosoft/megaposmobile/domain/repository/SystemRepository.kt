package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.AppVersion
import kotlinx.coroutines.flow.Flow

interface SystemRepository {
    suspend fun getServerAppVersion(): Flow<Resource<AppVersion>>
}
