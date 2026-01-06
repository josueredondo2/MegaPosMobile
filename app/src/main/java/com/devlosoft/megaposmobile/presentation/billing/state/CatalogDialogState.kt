package com.devlosoft.megaposmobile.presentation.billing.state

import com.devlosoft.megaposmobile.domain.model.CatalogItem
import com.devlosoft.megaposmobile.domain.model.CatalogType

data class CatalogDialogState(
    val isVisible: Boolean = false,

    // Categories
    val catalogTypes: List<CatalogType> = emptyList(),
    val selectedCatalogTypeId: Int? = null,
    val isLoadingTypes: Boolean = false,

    // Letter filter (default to 'A')
    val selectedLetter: Char = 'A',

    // Items
    val catalogItems: List<CatalogItem> = emptyList(),
    val isLoadingItems: Boolean = false,

    // Adding item state
    val isAddingItem: Boolean = false,

    // Error
    val error: String? = null
) {
    companion object {
        val LETTERS = ('A'..'Z').toList()
    }
}
