package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.InvoiceTotals
import com.google.gson.annotations.SerializedName

data class ChangeQuantityRequestDto(
    @SerializedName("itemPosId") val itemPosId: String,
    @SerializedName("lineNumber") val lineNumber: Int,
    @SerializedName("newQuantity") val newQuantity: Double,
    @SerializedName("partyAffiliationTypeCode") val partyAffiliationTypeCode: String,
    @SerializedName("isAuthorized") val isAuthorized: Boolean,
    @SerializedName("authorizedBy") val authorizedBy: String?
)

/**
 * Response from change-quantity endpoint.
 * Returns the updated transaction details (totals and items).
 */
data class ChangeQuantityResponseDto(
    @SerializedName("totals") val totals: InvoiceTotalsDto?,
    @SerializedName("items") val items: List<InvoiceItemDto>?
) {
    fun toDomain(): InvoiceData = InvoiceData(
        totals = totals?.toDomain() ?: InvoiceTotals(),
        items = items?.map { it.toDomain() } ?: emptyList()
    )
}
