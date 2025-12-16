package com.devlosoft.megaposmobile.domain.model

data class TransactionRecoveryResult(
    val canRecover: Boolean,
    val canCreate: Boolean,
    val reason: String,
    val transactionId: String?,
    val invoiceData: InvoiceData?,
    val customer: Customer?
)
