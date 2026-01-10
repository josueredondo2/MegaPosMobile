package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request para registrar eventos de auditoría.
 * POST /pos-api/v1/audit/log
 */
data class AuditLogRequestDto(
    @SerializedName("action")
    val action: String,

    @SerializedName("detail")
    val detail: String,

    @SerializedName("transactionId")
    val transactionId: String? = null
)
