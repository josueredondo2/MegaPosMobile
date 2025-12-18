package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.InvoiceItem
import com.devlosoft.megaposmobile.domain.model.InvoiceTotals
import com.google.gson.annotations.SerializedName

data class AddMaterialResponseDto(
    @SerializedName("requiresAuthorization")
    val requiresAuthorization: Boolean,

    @SerializedName("transactionId")
    val transactionId: String?,

    @SerializedName("invoiceData")
    val invoiceData: InvoiceDataDto?
)

data class InvoiceDataDto(
    @SerializedName("totals")
    val totals: InvoiceTotalsDto?,

    @SerializedName("items")
    val items: List<InvoiceItemDto>?
) {
    fun toDomain(): InvoiceData = InvoiceData(
        totals = totals?.toDomain() ?: InvoiceTotals(),
        items = items?.map { it.toDomain() } ?: emptyList()
    )
}

data class InvoiceTotalsDto(
    @SerializedName("total")
    val total: Double?,

    @SerializedName("subTotal")
    val subTotal: Double?,

    @SerializedName("tax")
    val tax: Double?,

    @SerializedName("balanceDue")
    val balanceDue: Double?,

    @SerializedName("totalItems")
    val totalItems: Double?,

    @SerializedName("totalSavings")
    val totalSavings: Double?,

    @SerializedName("sponsors")
    val sponsors: Double?
) {
    fun toDomain(): InvoiceTotals = InvoiceTotals(
        total = total ?: 0.0,
        subTotal = subTotal ?: 0.0,
        tax = tax ?: 0.0,
        balanceDue = balanceDue ?: 0.0,
        totalItems = totalItems ?: 0.0,
        totalSavings = totalSavings ?: 0.0,
        sponsors = sponsors ?: 0.0
    )
}

data class InvoiceItemDto(
    @SerializedName("lineItemSequence")
    val lineItemSequence: Int?,

    @SerializedName("itemId")
    val itemId: String?,

    @SerializedName("itemName")
    val itemName: String?,

    @SerializedName("discountPercentage")
    val discountPercentage: Double?,

    @SerializedName("unitPrice")
    val unitPrice: Double?,

    @SerializedName("quantity")
    val quantity: Double?,

    @SerializedName("total")
    val total: Double?,

    @SerializedName("hasDiscount")
    val hasDiscount: Boolean?,

    @SerializedName("isTaxExempt")
    val isTaxExempt: Boolean?,

    @SerializedName("previousPrice")
    val previousPrice: Double?,

    @SerializedName("priceModifierPercentage")
    val priceModifierPercentage: Double?,

    @SerializedName("newPrice")
    val newPrice: Double?,

    @SerializedName("isSponsor")
    val isSponsor: Boolean?,

    @SerializedName("isDeleted")
    val isDeleted: Boolean?,

    @SerializedName("packageItemId")
    val packageItemId: String?,

    @SerializedName("packageItemQuantity")
    val packageItemQuantity: Double?,

    @SerializedName("allowsDelete")
    val allowsDelete: Boolean?,

    @SerializedName("requiresAuthorization")
    val requiresAuthorization: Boolean?,

    @SerializedName("isTaxExonerated")
    val isTaxExonerated: Boolean?
) {
    fun toDomain(): InvoiceItem = InvoiceItem(
        lineItemSequence = lineItemSequence ?: 0,
        itemId = itemId ?: "",
        itemName = itemName ?: "",
        discountPercentage = discountPercentage ?: 0.0,
        unitPrice = unitPrice ?: 0.0,
        quantity = quantity ?: 0.0,
        total = total ?: 0.0,
        hasDiscount = hasDiscount ?: false,
        isDeleted = isDeleted ?: false,
        isSponsor = isSponsor ?: false,
        isTaxExempt = isTaxExempt ?: false,
        hasPackaging = !packageItemId.isNullOrBlank()
    )
}
