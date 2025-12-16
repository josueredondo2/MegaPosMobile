package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.TransactionRecoveryResult
import com.google.gson.annotations.SerializedName

data class CanRecoverTransactionResponseDto(
    @SerializedName("canRecover")
    val canRecover: Boolean,

    @SerializedName("canCreate")
    val canCreate: Boolean,

    @SerializedName("reason")
    val reason: String,

    @SerializedName("transactionId")
    val transactionId: String?,

    @SerializedName("transactionData")
    val transactionData: TransactionRecoveryDataDto?
) {
    fun toDomain(): TransactionRecoveryResult = TransactionRecoveryResult(
        canRecover = canRecover,
        canCreate = canCreate,
        reason = reason,
        transactionId = transactionId,
        invoiceData = transactionData?.invoiceData?.toDomain(),
        customer = transactionData?.customer?.toDomain()
    )
}

data class TransactionRecoveryDataDto(
    @SerializedName("totals")
    val totals: InvoiceTotalsDto?,

    @SerializedName("items")
    val items: List<InvoiceItemDto>?,

    @SerializedName("customer")
    val customer: CustomerInfoDto?
) {
    val invoiceData: InvoiceDataDto
        get() = InvoiceDataDto(totals = totals, items = items)
}

data class CustomerInfoDto(
    @SerializedName("customerId")
    val customerId: String?,

    @SerializedName("customerName")
    val customerName: String?
) {
    fun toDomain(): Customer = Customer(
        partyId = customerId?.toIntOrNull() ?: 0,
        partyType = "PERS",
        identification = customerId ?: "",
        identificationDescription = "",
        identificationType = "CI",
        name = customerName ?: "",
        affiliate = "",
        affiliateType = "0001",
        discountAmount = 0.0,
        percentageDiscount = 0.0,
        isValid = true
    )
}
