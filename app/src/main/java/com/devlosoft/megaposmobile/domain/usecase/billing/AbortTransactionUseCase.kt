package com.devlosoft.megaposmobile.domain.usecase.billing

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for aborting a transaction.
 * Handles the API call to abort and clearing local state.
 */
class AbortTransactionUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "AbortTransactionUseCase"
    }

    /**
     * Aborts a transaction.
     *
     * @param transactionId The transaction ID to abort
     * @param reason The reason for aborting the transaction
     * @param authorizingOperator The user who authorized the abort
     * @return Result.success(true) if aborted, Result.failure with error otherwise
     */
    suspend operator fun invoke(
        transactionId: String,
        reason: String,
        authorizingOperator: String
    ): Result<Boolean> {
        if (transactionId.isBlank()) {
            return Result.failure(AbortException("No hay transacción activa"))
        }

        if (reason.isBlank()) {
            return Result.failure(AbortException("Debe ingresar una razón para abortar"))
        }

        val sessionId = sessionManager.getSessionId().first()
        val workstationId = sessionManager.getStationId().first()

        if (sessionId.isNullOrBlank() || workstationId.isNullOrBlank()) {
            return Result.failure(AbortException("No hay sesión activa"))
        }

        return try {
            var abortResult: Result<Boolean> = Result.failure(AbortException("Error desconocido"))

            billingRepository.abortTransaction(
                sessionId = sessionId,
                workstationId = workstationId,
                transactionId = transactionId,
                reason = reason,
                authorizingOperator = authorizingOperator
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        Log.d(TAG, "Aborting transaction...")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "Transaction aborted successfully")
                        // Clear active transaction from local storage
                        billingRepository.clearActiveTransactionId()
                        abortResult = Result.success(true)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Failed to abort transaction: ${result.message}")
                        abortResult = Result.failure(AbortException(result.message ?: "Error al abortar la transacción"))
                    }
                }
            }

            abortResult
        } catch (e: Exception) {
            Log.e(TAG, "Exception in abortTransaction: ${e.message}", e)
            Result.failure(AbortException("Error: ${e.message}"))
        }
    }
}

/**
 * Exception thrown when abort fails
 */
class AbortException(message: String) : Exception(message)
