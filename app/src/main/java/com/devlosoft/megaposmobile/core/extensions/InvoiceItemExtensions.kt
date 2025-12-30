package com.devlosoft.megaposmobile.core.extensions

import com.devlosoft.megaposmobile.domain.model.InvoiceItem

/**
 * Extension functions for working with invoice items.
 */

/**
 * Filters items that should be visible in the UI:
 * - Excludes items marked as deleted
 * - Excludes orphaned packaging items (packaging whose nearest parent is deleted)
 *
 * @return List of visible invoice items
 */
fun List<InvoiceItem>.getVisibleItems(): List<InvoiceItem> {
    // Calculate orphaned packaging line sequences
    val orphanedLineSeqs = mutableSetOf<Int>()

    this.forEach { item ->
        // Check if this item is a packaging (some parent references it)
        val parentItems = this.filter { it.packagingItemId == item.itemId }
        if (parentItems.isNotEmpty()) {
            // This is a packaging item - find the nearest parent before this item
            val nearestParent = parentItems
                .filter { it.lineItemSequence < item.lineItemSequence }
                .maxByOrNull { it.lineItemSequence }

            if (nearestParent?.isDeleted == true) {
                orphanedLineSeqs.add(item.lineItemSequence)
            }
        }
    }

    // Return items that are NOT deleted AND NOT orphaned packaging
    return this.filter { item ->
        !item.isDeleted && !orphanedLineSeqs.contains(item.lineItemSequence)
    }
}

/**
 * Checks if the list has any items with packaging that needs reconciliation.
 *
 * @return true if there are non-deleted items with packaging
 */
fun List<InvoiceItem>.hasPackagingItems(): Boolean {
    return this.any { it.hasPackaging && !it.isDeleted }
}

/**
 * Gets the set of item IDs that are packaging items (referenced by parents).
 *
 * @return Set of packaging item IDs
 */
fun List<InvoiceItem>.getPackagingItemIds(): Set<String> {
    return this
        .filter { it.packagingItemId.isNotBlank() }
        .map { it.packagingItemId }
        .toSet()
}

/**
 * Checks if an item is a packaging item (referenced by other items).
 *
 * @param itemId The item ID to check
 * @return true if the item is a packaging item
 */
fun List<InvoiceItem>.isPackagingItem(itemId: String): Boolean {
    return this.getPackagingItemIds().contains(itemId)
}

/**
 * Calculates the total quantity of non-deleted items.
 *
 * @return Total quantity as Int
 */
fun List<InvoiceItem>.getTotalItemCount(): Int {
    return this.filter { !it.isDeleted }.sumOf { it.quantity }.toInt()
}
