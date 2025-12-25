package com.devlosoft.megaposmobile.domain.model

/**
 * Domain model representing a packaging/returnable container item
 * associated with products in a transaction.
 */
data class PackagingItem(
    val itemPosId: String,
    val description: String,
    val quantityInvoiced: Double,  // Facturado (QU_PND) - how many packagings the customer should return
    val quantityRedeemed: Double,  // Ya entregado (QU_RED) - how many already returned
    val quantityToCharge: Double   // A cobrar - pending to return (quantityInvoiced - quantityRedeemed)
)
