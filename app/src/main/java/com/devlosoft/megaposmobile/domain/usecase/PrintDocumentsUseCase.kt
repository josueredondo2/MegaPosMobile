package com.devlosoft.megaposmobile.domain.usecase

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.domain.model.PrintDocument
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Use case for fetching and printing transaction documents.
 * Centralizes the printing logic that was duplicated in BillingViewModel and ProcessViewModel.
 */
class PrintDocumentsUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
    private val printerManager: PrinterManager,
    private val serverConfigDao: ServerConfigDao
) {
    companion object {
        private const val TAG = "PrintDocumentsUseCase"
        private const val DEFAULT_TEMPLATE_ID = "01-FC"
    }

    /**
     * Fetches and prints all documents for a transaction.
     *
     * @param transactionId The transaction ID to print documents for
     * @param templateId Optional template ID (defaults to "01-FC")
     * @param isReprint Whether this is a reprint (defaults to false)
     * @param copyNumber The copy number (defaults to 0)
     * @return Result.success with the number of documents printed, or Result.failure with error
     */
    suspend operator fun invoke(
        transactionId: String,
        templateId: String = DEFAULT_TEMPLATE_ID,
        isReprint: Boolean = false,
        copyNumber: Int = 0
    ): Result<Int> {
        Log.d(TAG, "Fetching and printing documents for transaction: $transactionId")

        return try {
            var printedCount = 0
            var lastError: Exception? = null

            billingRepository.getPrintDocuments(
                transactionId = transactionId,
                templateId = templateId,
                isReprint = isReprint,
                copyNumber = copyNumber
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "Fetching print documents...")
                    }
                    is Resource.Success -> {
                        val documents = result.data ?: emptyList()
                        Log.d(TAG, "Received ${documents.size} print documents")

                        // Print each document
                        for (document in documents) {
                            try {
                                printDocument(document)
                                printedCount++
                            } catch (e: Exception) {
                                Log.e(TAG, "Error printing document: ${e.message}")
                                // Use PrinterFailureException to indicate documents were retrieved but printing failed
                                lastError = PrinterFailureException(e.message ?: "Error de impresora")
                            }
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error fetching print documents: ${result.message}")
                        lastError = PrintException(result.message ?: "Error al obtener documentos de impresión")
                    }
                }
            }

            if (printedCount > 0) {
                Log.d(TAG, "Successfully printed $printedCount documents")
                Result.success(printedCount)
            } else if (lastError != null) {
                Result.failure(lastError!!)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in PrintDocumentsUseCase: ${e.message}", e)
            Result.failure(PrintException("Error al imprimir documentos: ${e.message}"))
        }
    }

    private suspend fun printDocument(document: PrintDocument) {
        Log.d(TAG, "Printing document: ${document.documentType}")

        val config = serverConfigDao.getActiveServerConfigSync()

        if (config == null) {
            Log.e(TAG, "No server config found")
            throw PrintException("Configuración de servidor no encontrada")
        }

        val printText = document.printText
        Log.d(TAG, "Print text length: ${printText.length}")

        val printerModel = PrinterModel.fromString(config.printerModel)
        Log.d(TAG, "Using printer model: ${printerModel.displayName}")

        // Print using PrinterManager (handles both IP and Bluetooth internally)
        val result = printerManager.printText(printText)

        result.fold(
            onSuccess = { message ->
                Log.d(TAG, "Print success: $message")
            },
            onFailure = { exception ->
                Log.e(TAG, "Print error: ${exception.message}")
                throw PrintException("Error al imprimir: ${exception.message}")
            }
        )
    }
}

/**
 * Exception thrown when printing fails
 */
class PrintException(message: String) : Exception(message)

/**
 * Exception thrown when the API returns documents successfully but the printer fails.
 * This is used to distinguish between API errors and printer errors for retry logic.
 */
class PrinterFailureException(message: String) : Exception(message)
