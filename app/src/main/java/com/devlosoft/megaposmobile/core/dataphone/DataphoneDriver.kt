package com.devlosoft.megaposmobile.core.dataphone

import com.devlosoft.megaposmobile.domain.model.DatafonoProvider
import com.devlosoft.megaposmobile.domain.model.DataphonePaymentResult

/**
 * Interface para drivers de datáfono.
 * Cada proveedor debe implementar esta interface.
 */
interface DataphoneDriver {
    /**
     * Proveedor de datáfono que maneja este driver.
     */
    fun getProvider(): DatafonoProvider

    /**
     * Construye la URL de request para procesar un pago.
     * @param baseUrl URL base del datáfono (ej: "http://192.168.18.54:8080")
     * @param amount Monto en colones (sin decimales)
     * @return URL completa para llamar al datáfono
     */
    fun buildRequestUrl(baseUrl: String, amount: Long): String

    /**
     * Parsea la respuesta JSON del datáfono a un resultado normalizado.
     * @param jsonResponse JSON crudo devuelto por el datáfono
     * @return Resultado del pago normalizado
     */
    fun parseResponse(jsonResponse: String): DataphonePaymentResult
}
