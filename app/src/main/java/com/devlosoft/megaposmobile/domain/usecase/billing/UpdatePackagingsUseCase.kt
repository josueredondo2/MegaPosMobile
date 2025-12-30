package com.devlosoft.megaposmobile.domain.usecase.billing

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.dto.PackagingItemDto
import com.devlosoft.megaposmobile.domain.model.PackagingItem
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Use case for updating packaging quantities in a transaction.
 */
class UpdatePackagingsUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    companion object {
        private const val TAG = "UpdatePackagingsUseCase"
    }

    /**
     * Input for a packaging update
     */
    data class PackagingUpdate(
        val itemPosId: String,
        val quantity: Double
    )

    /**
     * Updates packaging quantities for a transaction.
     *
     * @param transactionId The transaction ID
     * @param packagingItems The list of packaging items with their quantities
     * @param packagingInputs Map of itemPosId to input quantity string
     * @param affiliateType The affiliate type code
     * @return Result.success(true) on success, or failure with error
     */
    suspend operator fun invoke(
        transactionId: String,
        packagingItems: List<PackagingItem>,
        packagingInputs: Map<String, String>,
        affiliateType: String
    ): Result<Boolean> {
        if (transactionId.isBlank()) {
            return Result.failure(PackagingException("No hay transacción activa"))
        }

        // Build list of packagings to update (only those with quantity > 0)
        val packagingsToUpdate = packagingItems.mapNotNull { item ->
            val inputValue = packagingInputs[item.itemPosId]?.toDoubleOrNull() ?: 0.0
            if (inputValue > 0) {
                // Validate quantity doesn't exceed available
                if (inputValue > item.quantityInvoiced) {
                    return Result.failure(
                        PackagingException("La cantidad para ${item.description} no puede exceder ${item.quantityInvoiced.toInt()}")
                    )
                }
                PackagingItemDto(itemPosId = item.itemPosId, quantity = inputValue)
            } else {
                null
            }
        }

        if (packagingsToUpdate.isEmpty()) {
            return Result.failure(PackagingException("Debe ingresar al menos un envase"))
        }

        return try {
            var result: Result<Boolean> = Result.failure(PackagingException("Error desconocido"))

            billingRepository.updatePackagings(
                transactionId = transactionId,
                packagings = packagingsToUpdate,
                affiliateType = affiliateType
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "Updating packagings...")
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "Packagings updated successfully")
                        result = Result.success(true)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Failed to update packagings: ${resource.message}")
                        result = Result.failure(PackagingException(resource.message ?: "Error al actualizar empaques"))
                    }
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating packagings: ${e.message}", e)
            Result.failure(PackagingException("Error: ${e.message}"))
        }
    }
}
