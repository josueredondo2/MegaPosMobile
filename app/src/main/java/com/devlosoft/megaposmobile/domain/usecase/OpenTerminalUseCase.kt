package com.devlosoft.megaposmobile.domain.usecase

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.util.DeviceIdentifier
import com.devlosoft.megaposmobile.domain.model.OpenStationResult
import com.devlosoft.megaposmobile.domain.repository.CashierStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class OpenTerminalUseCase @Inject constructor(
    private val cashierStationRepository: CashierStationRepository,
    private val deviceIdentifier: DeviceIdentifier
) {
    suspend operator fun invoke(): Flow<Resource<OpenStationResult>> {
        val deviceId = deviceIdentifier.getDeviceId()

        if (deviceId.isBlank()) {
            return flow {
                emit(Resource.Error("No se pudo obtener el identificador del dispositivo"))
            }
        }

        return cashierStationRepository.openStation(deviceId)
    }
}
