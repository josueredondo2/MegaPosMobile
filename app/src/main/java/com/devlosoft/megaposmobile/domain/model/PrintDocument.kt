package com.devlosoft.megaposmobile.domain.model

data class PrintDocument(
    val documentType: String,
    val printText: String,
    val couponNumber: String?,
    val promotionName: String?
)
