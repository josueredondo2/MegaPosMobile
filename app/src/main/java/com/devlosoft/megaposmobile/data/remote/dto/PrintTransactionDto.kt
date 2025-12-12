package com.devlosoft.megaposmobile.data.remote.dto

data class PrintTransactionResponseDto(
    val documents: List<PrintDocumentDto>
)

data class PrintDocumentDto(
    val documentType: String,
    val printText: String,
    val couponNumber: String?,
    val promotionName: String?
)
