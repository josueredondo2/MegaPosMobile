package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class ErrorResponseDto(
    @SerializedName("errorCode") val errorCode: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("statusCode") val statusCode: Int?
) {
    companion object {
        private val gson = Gson()

        fun fromJson(json: String?): ErrorResponseDto? {
            return try {
                json?.let { gson.fromJson(it, ErrorResponseDto::class.java) }
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Maps backend error codes to Spanish user-friendly messages
         */
        fun getSpanishMessage(errorCode: String?): String {
            return when (errorCode) {
                // Customer errors
                "CUSTOMER_NOT_FOUND" -> "Cliente no encontrado"

                // Item/Material errors
                "ITEM_NOT_FOUND" -> "Artículo no encontrado"
                "ITEM_NOT_AVAILABLE_FOR_SALE" -> "Artículo no disponible para la venta"
                "ITEM_REQUIRES_AUTHORIZATION" -> "Artículo requiere autorización"
                "ITEM_MUST_SELL_SEPARATELY" -> "El artículo debe venderse por separado"
                "ITEM_MINIMUM_QUANTITY_NOT_MET" -> "No se cumple la cantidad mínima del artículo"
                "ITEM_REQUIRES_WEIGHING" -> "El artículo requiere ser pesado"
                "ITEM_MAXIMUM_QUANTITY_EXCEEDED" -> "Se excedió la cantidad máxima del artículo"

                // Transaction errors
                "TRANSACTION_NOT_FOUND" -> "Transacción no encontrada"
                "TRANSACTION_HAS_NO_ITEMS" -> "La transacción no tiene artículos"
                "TRANSACTION_HAS_PENDING_BALANCE" -> "La transacción tiene saldo pendiente"
                "TRANSACTION_ALREADY_PAID" -> "La transacción ya fue pagada"
                "TRANSACTION_ID_REQUIRED" -> "Se requiere el ID de transacción"
                "ITEM_POS_ID_REQUIRED" -> "Se requiere el código del artículo"
                "INVALID_LINE_NUMBER" -> "Número de línea inválido"
                "INVALID_QUANTITY" -> "Cantidad inválida"

                // Session errors
                "SESSION_CLOSED" -> "La sesión está cerrada"
                "SESSION_NOT_FOUND" -> "Sesión no encontrada"
                "SESSION_VALIDATION_ERROR" -> "Error de validación de sesión"

                // Station errors
                "STATION_NOT_FOUND" -> "Terminal no encontrada"
                "BUSINESS_DAY_CLOSED" -> "El día comercial está cerrado"
                "BUSINESS_DAY_NOT_OPEN" -> "El día comercial no está abierto"

                // Database errors
                "DATABASE_TIMEOUT" -> "Tiempo de espera agotado. Intente nuevamente"
                "DATABASE_CONNECTION_ERROR" -> "Error de conexión con el servidor"
                "DATABASE_UNAVAILABLE" -> "El servidor no está disponible"

                // Generic errors
                "UNKNOWN_ERROR" -> "Error desconocido"

                else -> "Error: ${errorCode ?: "desconocido"}"
            }
        }
    }
}
