package com.devlosoft.megaposmobile.domain.model

data class TodayTransaction(
    val transactionId: String,
    val customerName: String?
) {
    val displayCustomerName: String
        get() = customerName ?: "General"
}
