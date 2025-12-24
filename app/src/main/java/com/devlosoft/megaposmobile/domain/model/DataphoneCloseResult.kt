package com.devlosoft.megaposmobile.domain.model

/**
 * Resultado normalizado del cierre de datafono.
 * Contiene los datos extraidos del TICKET del PAX.
 */
data class DataphoneCloseResult(
    val success: Boolean,
    val terminal: String?,
    val batchNumber: String?,
    val salesCount: Int,
    val salesTotal: Double,
    val reversalsCount: Int,
    val reversalsTotal: Double,
    val netTotal: Double,
    val ticket: String?,
    val errorMessage: String? = null
)
