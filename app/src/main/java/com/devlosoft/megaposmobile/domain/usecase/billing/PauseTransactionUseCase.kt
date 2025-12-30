package com.devlosoft.megaposmobile.domain.usecase.billing

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.printer.LocalPrintTemplates
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for pausing a transaction.
 * Handles the API call to pause, clearing local state, and printing the pause receipt.
 */
class PauseTransactionUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
    private val sessionManager: SessionManager,
    private val printerManager: PrinterManager
) {
    companion object {
        private const val TAG = "PauseTransactionUseCase"
    }

    /**
     * Result of pausing a transaction
     */
    sealed class PauseResult {
        /**
         * Transaction paused and receipt printed successfully
         */
        data object Success : PauseResult()

        /**
         * Transaction paused but receipt printing failed
         * @param printError The error message from the print failure
         * @param printText The text that failed to print (for retry)
         */
        data class PrintFailed(val printError: String, val printText: String) : PauseResult()

        /**
         * Pausing the transaction failed
         * @param message Error message describing why the pause failed
         */
        data class Failed(val message: String) : PauseResult()
    }

    /**
     * Pauses a transaction and prints a receipt.
     *
     * @param transactionId The transaction ID to pause
     * @param totalItems Total number of items in the transaction
     * @param subtotal The subtotal amount
     * @return PauseResult indicating success, print failure, or complete failure
     */
    suspend operator fun invoke(
        transactionId: String,
        totalItems: Int,
        subtotal: Double
    ): PauseResult {
        if (transactionId.isBlank()) {
            return PauseResult.Failed("No hay transacción activa")
        }

        val sessionId = sessionManager.getSessionId().first()
        val workstationId = sessionManager.getStationId().first()

        if (sessionId.isNullOrBlank() || workstationId.isNullOrBlank()) {
            return PauseResult.Failed("No hay sesión activa")
        }

        return try {
            // Call API to pause transaction
            var pauseResult: PauseResult = PauseResult.Failed("Error desconocido")

            billingRepository.pauseTransaction(
                transactionId = transactionId,
                sessionId = sessionId,
                workstationId = workstationId
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "Pausing transaction...")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "Transaction paused successfully")
                        // Clear active transaction from local storage
                        billingRepository.clearActiveTransactionId()

                        // Generate and print receipt
                        pauseResult = printPauseReceipt(transactionId, totalItems, subtotal)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Failed to pause transaction: ${result.message}")
                        pauseResult = PauseResult.Failed(result.message ?: "Error al pausar la transacción")
                    }
                }
            }

            pauseResult
        } catch (e: Exception) {
            Log.e(TAG, "Exception in pauseTransaction: ${e.message}", e)
            PauseResult.Failed("Error: ${e.message}")
        }
    }

    private suspend fun printPauseReceipt(
        transactionId: String,
        totalItems: Int,
        subtotal: Double
    ): PauseResult {
        return try {
            val userName = sessionManager.getUserName().first() ?: "Usuario"
            val businessUnitName = sessionManager.getBusinessUnitName().first() ?: "Megasuper"

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
                        PauseResult.Success
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to print pause receipt: ${error.message}")
                        PauseResult.PrintFailed(
                            printError = error.message ?: "Error al imprimir",
                            printText = printText
                        )
                    }
                )
        } catch (e: Exception) {
            Log.e(TAG, "Exception printing pause receipt: ${e.message}", e)
            PauseResult.PrintFailed(
                printError = "Error: ${e.message}",
                printText = ""
            )
        }
    }
}
