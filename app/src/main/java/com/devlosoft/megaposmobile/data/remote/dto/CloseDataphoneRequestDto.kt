package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request para enviar al endpoint de cierre de datafono en megapos.
 * POST /pos-api/v1/payment/close-dataphone
 */
data class CloseDataphoneRequestDto(
    @SerializedName("acquirerCode")
    val acquirerCode: String = "BAC",

    @SerializedName("pointOfSaleCode")
    val pointOfSaleCode: String,

    @SerializedName("terminalId")
    val terminalId: String?,

    @SerializedName("paxResponse")
    val paxResponse: PaxCloseResponseDto?
)
