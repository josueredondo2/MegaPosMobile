package com.devlosoft.megaposmobile.domain.usecase

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.model.CloseStationResult
import com.devlosoft.megaposmobile.domain.repository.CashierStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CloseTerminalUseCase @Inject constructor(
    private val cashierStationRepository: CashierStationRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Flow<Resource<CloseStationResult>> {
        val sessionId = sessionManager.getSessionId().first()

        if (sessionId.isNullOrBlank()) {
            return flow {
                emit(Resource.Error("No hay sesión activa para cerrar"))
            }
        }

        return cashierStationRepository.closeStation(sessionId)
    }
}
