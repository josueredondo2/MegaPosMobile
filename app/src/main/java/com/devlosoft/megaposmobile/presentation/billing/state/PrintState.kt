package com.devlosoft.megaposmobile.presentation.billing.state

/**
 * State for print operations.
 * This groups related state fields for printing.
 */
data class PrintState(
    val isPrinting: Boolean = false,
    val showPrintErrorDialog: Boolean = false,
    val printErrorMessage: String? = null,
    val pendingPrintText: String? = null,          // For pause receipt retry
    val pendingPrintTransactionCode: String? = null, // For finalize documents retry
    // Flag to track if documents were retrieved from API before print failed
    // Used to determine isReprint value on retry: true if API succeeded but printer failed
    val documentsRetrievedBeforeFail: Boolean = false
)
