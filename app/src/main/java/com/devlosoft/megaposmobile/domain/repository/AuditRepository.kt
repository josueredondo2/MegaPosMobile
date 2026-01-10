package com.devlosoft.megaposmobile.domain.repository

/**
 * Repository para operaciones de auditoría/bitácora.
 */
interface AuditRepository {
    /**
     * Registra un evento de auditoría.
     * @param action Acción realizada (ej: "ADD")
     * @param detail Descripción del evento
     * @param transactionId ID de la transacción (opcional)
     */
    suspend fun log(action: String, detail: String, transactionId: String? = null)
}
