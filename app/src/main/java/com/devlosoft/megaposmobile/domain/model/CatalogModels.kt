package com.devlosoft.megaposmobile.domain.model

data class CatalogType(
    val catalogTypeId: Int,
    val catalogName: String
)

data class CatalogItem(
    val unitSalesType: Int?,
    val catalogItemName: String,
    val catalogItemImage: String?,
    val itemPosId: String
)
