package com.devlosoft.megaposmobile.presentation.billing.state

/**
 * State for transaction control operations (pause and abort).
 * This groups related state fields for better organization.
 */
data class TransactionControlState(
    // Pause transaction state
    val showPauseConfirmDialog: Boolean = false,
    val isPausingTransaction: Boolean = false,
    val pauseTransactionError: String? = null,
    val shouldNavigateAfterPause: Boolean = false,

    // Abort transaction state
    val showAbortConfirmDialog: Boolean = false,
    val abortReason: String = "",
    val abortAuthorizingOperator: String = "",
    val isAbortingTransaction: Boolean = false,
    val abortTransactionError: String? = null,
    val shouldNavigateAfterAbort: Boolean = false
)
