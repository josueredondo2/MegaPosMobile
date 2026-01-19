package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.dto.CloseDataphoneResponseDto
import com.devlosoft.megaposmobile.data.remote.dto.DataphoneDataDto
import com.devlosoft.megaposmobile.data.remote.dto.PaxCloseResponseDto
import kotlinx.coroutines.flow.Flow

/**
 * Repository para operaciones de pago y cierre de datafono.
 */
interface PaymentRepository {
    /**
     * Procesa el pago con datáfono (llama al dispositivo PAX).
     * @param transactionId ID de la transacción para audit log
     * @param amount Monto a cobrar
     * @return Datos del pago procesado por el datáfono
     */
    suspend fun processDataphonePayment(
        transactionId: String,
        amount: Double
    ): Flow<Resource<DataphoneDataDto>>

    /**
     * Envia el cierre de datafono a megapos.
     * @param pointOfSaleCode Codigo del punto de venta
     * @param terminalId Codigo del terminal/datafono
     * @param paxResponse Respuesta del PAX al hacer cierre
     * @param sessionId ID de la sesión del terminal
     * @param workstationId ID de la estación de trabajo
     * @return Respuesta del servidor con los totales del cierre
     */
    suspend fun closeDataphone(
        pointOfSaleCode: String,
        terminalId: String?,
        paxResponse: PaxCloseResponseDto?,
        sessionId: String,
        workstationId: String
    ): Flow<Resource<CloseDataphoneResponseDto>>
}
