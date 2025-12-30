package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.TodayTransaction

data class TodayTransactionDto(
    val transactionId: String,
    val customerName: String?
) {
    fun toDomain(): TodayTransaction = TodayTransaction(
        transactionId = transactionId,
        customerName = customerName
    )
}
