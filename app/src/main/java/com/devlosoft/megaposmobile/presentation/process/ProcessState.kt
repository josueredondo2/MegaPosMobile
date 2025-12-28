package com.devlosoft.megaposmobile.presentation.process

sealed class ProcessStatus {
    data object Loading : ProcessStatus()
    data class Success(val message: String) : ProcessStatus()
    data class Error(val message: String) : ProcessStatus()
    // Print error: transaction succeeded but print failed - allows retry or skip
    data class PrintError(val message: String) : ProcessStatus()
}

data class ProcessState(
    val status: ProcessStatus = ProcessStatus.Loading,
    val loadingMessage: String = "",
    // For retry print after payment
    val pendingPrintTransactionCode: String? = null,
    val isPrinting: Boolean = false,
    // Flag to track if documents were retrieved from API before print failed
    // Used to determine isReprint value on retry: true if API succeeded but printer failed
    val documentsRetrievedBeforeFail: Boolean = false
)
