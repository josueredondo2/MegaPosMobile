package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.CloseStationResult
import com.devlosoft.megaposmobile.domain.model.OpenStationResult
import kotlinx.coroutines.flow.Flow

interface CashierStationRepository {
    suspend fun openStation(deviceId: String): Flow<Resource<OpenStationResult>>
    suspend fun closeStation(sessionId: String): Flow<Resource<CloseStationResult>>
}
