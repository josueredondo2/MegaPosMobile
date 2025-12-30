package com.devlosoft.megaposmobile.domain.usecase.billing

import android.util.Log
import com.devlosoft.megaposmobile.core.printer.LocalPrintTemplates
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for printing a pause receipt.
 * Generates and prints the receipt text for a paused transaction.
 */
class PrintPauseReceiptUseCase @Inject constructor(
    private val printerManager: PrinterManager,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "PrintPauseReceiptUseCase"
    }

    /**
     * Result of printing a pause receipt
     */
    sealed class PrintResult {
        /**
         * Print successful
         */
        data object Success : PrintResult()

        /**
         * Print failed
         * @param error The error message
         * @param printText The text that failed to print (for retry)
         */
        data class Failed(val error: String, val printText: String) : PrintResult()
    }

    /**
     * Generates and prints a pause receipt.
     *
     * @param transactionId The transaction ID
     * @param totalItems Total number of items in the transaction
     * @param subtotal The subtotal amount
     * @return PrintResult indicating success or failure
     */
    suspend operator fun invoke(
        transactionId: String,
        totalItems: Int,
        subtotal: Double
    ): PrintResult {
        return try {
            val userName = sessionManager.getUserName().first() ?: "Usuario"
            val businessUnitName = sessionManager.getBusinessUnitName().first() ?: "Megasuper"

            Log.d(TAG, "Generating pause receipt for user: $userName, businessUnit: $businessUnitName")

            val printText = LocalPrintTemplates.buildPendingTransactionReceipt(
                userName = userName,
                totalItems = totalItems,
                subtotal = subtotal,
                transactionId = transactionId,
                businessUnitName = businessUnitName
            )

            printerManager.printText(printText)
                .fold(
                    onSuccess = {
                        Log.d(TAG, "Pause receipt printed successfully")
                        PrintResult.Success
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to print pause receipt: ${error.message}")
                        PrintResult.Failed(
                            error = error.message ?: "Error al imprimir",
                            printText = printText
                        )
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Exception printing pause receipt: ${e.message}", e)
            PrintResult.Failed(
                error = "Error: ${e.message}",
                printText = ""
            )
        }
    }

    /**
     * Retries printing with previously generated text.
     *
     * @param printText The text to print
     * @return PrintResult indicating success or failure
     */
    suspend fun retry(printText: String): PrintResult {
        return try {
            printerManager.printText(printText)
                .fold(
                    onSuccess = {
                        Log.d(TAG, "Retry print successful")
                        PrintResult.Success
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Retry print failed: ${error.message}")
                        PrintResult.Failed(
                            error = error.message ?: "Error al imprimir",
                            printText = printText
                        )
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Exception in retry print: ${e.message}", e)
            PrintResult.Failed(
                error = "Error: ${e.message}",
                printText = printText
            )
        }
    }
}
