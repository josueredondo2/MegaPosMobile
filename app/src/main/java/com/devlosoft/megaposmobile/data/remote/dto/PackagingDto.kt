package com.devlosoft.megaposmobile.data.remote.dto

import com.devlosoft.megaposmobile.domain.model.PackagingItem
import com.google.gson.annotations.SerializedName

/**
 * Response DTO for GET /material/{transactionId}/packaging-reconciliation
 * Note: .NET serializes with camelCase, so ID_ITM_PS becomes iD_ITM_PS
 */
data class PackagingReconciliationDto(
    @SerializedName("iD_ITM_PS") val itemPosId: String?,
    @SerializedName("z_DE_ITM") val description: String?,
    @SerializedName("qU_PND") val quantityPending: Double?,
    @SerializedName("qU_RED") val quantityRedeemed: Double?
) {
    fun toDomain(): PackagingItem {
        val pending = quantityPending ?: 0.0
        val redeemed = quantityRedeemed ?: 0.0
        return PackagingItem(
            itemPosId = itemPosId?.trim() ?: "",
            description = description?.trim() ?: "Sin descripción",
            quantityInvoiced = pending,
            quantityRedeemed = redeemed,
            quantityToCharge = pending - redeemed
        )
    }
}

/**
 * Request DTO for POST /material/{transactionId}/update-packagings
 */
data class UpdatePackagingsRequestDto(
    val packagings: List<PackagingItemDto>,
    val affiliateType: String
)

/**
 * Individual packaging item for update request
 */
data class PackagingItemDto(
    val itemPosId: String,
    val quantity: Double
)
