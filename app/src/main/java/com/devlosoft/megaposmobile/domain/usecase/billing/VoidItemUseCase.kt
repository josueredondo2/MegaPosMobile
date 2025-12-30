package com.devlosoft.megaposmobile.domain.usecase.billing

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Use case for voiding (deleting) an item from a transaction.
 */
class VoidItemUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    companion object {
        private const val TAG = "VoidItemUseCase"
    }

    /**
     * Voids an item from a transaction.
     *
     * @param transactionId The transaction ID
     * @param itemPosId The item POS ID to void
     * @param authorizedOperator The user who authorized the void
     * @param affiliateType The affiliate type code
     * @param deleteAll Whether to delete all occurrences
     * @return Result.success(true) on success, or failure with error
     */
    suspend operator fun invoke(
        transactionId: String,
        itemPosId: String,
        authorizedOperator: String,
        affiliateType: String,
        deleteAll: Boolean = true
    ): Result<Boolean> {
        if (transactionId.isBlank()) {
            return Result.failure(VoidItemException("No hay transacción activa"))
        }

        return try {
            var result: Result<Boolean> = Result.failure(VoidItemException("Error desconocido"))

            billingRepository.voidItem(
                transactionId = transactionId,
                itemPosId = itemPosId,
                authorizedOperator = authorizedOperator,
                affiliateType = affiliateType,
                deleteAll = deleteAll
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "Voiding item...")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "Item voided successfully")
                        result = Result.success(true)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Failed to void item: ${resource.message}")
                        result = Result.failure(VoidItemException(resource.message ?: "Error al eliminar línea"))
                    }
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception voiding item: ${e.message}", e)
            Result.failure(VoidItemException("Error: ${e.message}"))
        }
    }
}

/**
 * Exception thrown when void item fails
 */
class VoidItemException(message: String) : Exception(message)
