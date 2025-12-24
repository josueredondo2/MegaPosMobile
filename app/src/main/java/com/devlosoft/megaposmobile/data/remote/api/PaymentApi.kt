package com.devlosoft.megaposmobile.data.remote.api

import com.devlosoft.megaposmobile.data.remote.dto.CloseDataphoneRequestDto
import com.devlosoft.megaposmobile.data.remote.dto.CloseDataphoneResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API para operaciones de pago y cierre de datafono.
 */
interface PaymentApi {

    /**
     * Ejecuta el cierre de lote del datafono en megapos.
     * Guarda el registro en ser_cierre_adquirente y marca las transacciones cerradas.
     */
    @POST("payment/close-dataphone")
    suspend fun closeDataphone(
        @Body request: CloseDataphoneRequestDto
    ): Response<CloseDataphoneResponseDto>
}
