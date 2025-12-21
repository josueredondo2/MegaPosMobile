package com.devlosoft.megaposmobile.core.dataphone

import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult

/**
 * Interface del servicio de datáfono.
 * Define las operaciones disponibles con el datáfono.
 */
interface DataphoneService {
    /**
     * Procesa un pago en el datáfono.
     * @param amount Monto en colones (sin decimales)
     * @return Resultado del pago
     */
    suspend fun processPayment(amount: Long): Result<DataphonePaymentResult>

    /**
     * Prueba la conexión con el datáfono.
     * @return Mensaje de estado de la conexión
     */
    suspend fun testConnection(): Result<String>
}
