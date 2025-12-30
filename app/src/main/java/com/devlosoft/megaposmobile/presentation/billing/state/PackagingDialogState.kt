package com.devlosoft.megaposmobile.presentation.billing.state

import com.devlosoft.megaposmobile.domain.model.PackagingItem

/**
 * State for the packaging reconciliation dialog.
 * This groups related state fields for the packaging dialog.
 */
data class PackagingDialogState(
    val isVisible: Boolean = false,
    val items: List<PackagingItem> = emptyList(),
    val inputs: Map<String, String> = emptyMap(), // itemPosId -> input value
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val isUpdating: Boolean = false,
    val updateError: String? = null
)
