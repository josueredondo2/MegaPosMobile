package com.devlosoft.megaposmobile.domain.usecase.billing

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.PackagingItem
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Use case for fetching packaging reconciliation items for a transaction.
 */
class GetPackagingReconciliationUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    companion object {
        private const val TAG = "GetPackagingReconciliation"
    }

    /**
     * Fetches the packaging items that need reconciliation for a transaction.
     *
     * @param transactionId The transaction ID to get packaging items for
     * @return Result with list of PackagingItem on success, or failure with error
     */
    suspend operator fun invoke(transactionId: String): Result<List<PackagingItem>> {
        if (transactionId.isBlank()) {
            return Result.failure(PackagingException("No hay transacción activa"))
        }

        return try {
            var result: Result<List<PackagingItem>> = Result.failure(PackagingException("Error desconocido"))

            billingRepository.getPackagingReconciliation(transactionId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "Loading packaging items...")
                    }
                    is Resource.Success -> {
                        val items = resource.data ?: emptyList()
                        Log.d(TAG, "Loaded ${items.size} packaging items")
                        result = Result.success(items)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Failed to load packaging items: ${resource.message}")
                        result = Result.failure(PackagingException(resource.message ?: "Error al cargar empaques"))
                    }
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading packaging items: ${e.message}", e)
            Result.failure(PackagingException("Error: ${e.message}"))
        }
    }
}

/**
 * Exception thrown when packaging operations fail
 */
class PackagingException(message: String) : Exception(message)
