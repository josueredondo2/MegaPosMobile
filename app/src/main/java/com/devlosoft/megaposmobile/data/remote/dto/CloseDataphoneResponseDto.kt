package com.devlosoft.megaposmobile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response del endpoint de cierre de datafono en megapos.
 * POST /pos-api/v1/payment/close-dataphone
 */
data class CloseDataphoneResponseDto(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("closeId")
    val closeId: String?,

    @SerializedName("terminalId")
    val terminalId: String?,

    @SerializedName("batchNumber")
    val batchNumber: String?,

    @SerializedName("closeDate")
    val closeDate: String?,

    @SerializedName("salesCount")
    val salesCount: Int,

    @SerializedName("salesTotal")
    val salesTotal: Double,

    @SerializedName("reversalsCount")
    val reversalsCount: Int,

    @SerializedName("reversalsTotal")
    val reversalsTotal: Double,

    @SerializedName("netTotal")
    val netTotal: Double,

    @SerializedName("voucher")
    val voucher: String?,

    @SerializedName("closedTransactions")
    val closedTransactions: List<ClosedTransactionSummaryDto>? = null
)
