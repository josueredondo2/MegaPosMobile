package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.dto.CloseDataphoneResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.PaxCloseResponseDto
import kotlinx.coroutines.flow.Flow

/**
 * Repository para operaciones de pago y cierre de datafono.
 */
interface PaymentRepository {
    /**
     * Envia el cierre de datafono a megapos.
     * @param pointOfSaleCode Codigo del punto de venta
     * @param terminalId Codigo del terminal/datafono
     * @param paxResponse Respuesta del PAX al hacer cierre
     * @return Respuesta del servidor con los totales del cierre
     */
    suspend fun closeDataphone(
        pointOfSaleCode: String,
        terminalId: String?,
        paxResponse: PaxCloseResponseDto?
    ): Flow<Resource<CloseDataphoneResponseDto>>
}
