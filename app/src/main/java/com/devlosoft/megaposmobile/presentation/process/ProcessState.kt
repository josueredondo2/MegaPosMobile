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
    val isPrinting: Boolean = false
)
