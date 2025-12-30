package com.devlosoft.megaposmobile.domain.usecase.billing

import android.util.Log
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import javax.inject.Inject

/**
 * Use case for changing the quantity of an item in a transaction.
 */
class ChangeQuantityUseCase @Inject constructor(
    private val billingRepository: BillingRepository
) {
    companion object {
        private const val TAG = "ChangeQuantityUseCase"
        const val MIN_QUANTITY = 1.0
        const val MAX_QUANTITY = 99.0
    }

    /**
     * Changes the quantity of an item in a transaction.
     *
     * @param transactionId The transaction ID
     * @param itemPosId The item POS ID to update
     * @param lineNumber The line number of the item
     * @param newQuantity The new quantity
     * @param affiliateType The affiliate type code
     * @param authorizedBy The user who authorized the change (null if user has permission)
     * @return Result with updated InvoiceData on success, or failure with error
     */
    suspend operator fun invoke(
        transactionId: String,
        itemPosId: String,
        lineNumber: Int,
        newQuantity: Double,
        affiliateType: String,
        authorizedBy: String?
    ): Result<InvoiceData> {
        if (transactionId.isBlank()) {
            return Result.failure(ChangeQuantityException("No hay transacción activa"))
        }

        if (newQuantity < MIN_QUANTITY || newQuantity > MAX_QUANTITY) {
            return Result.failure(ChangeQuantityException("La cantidad debe ser entre $MIN_QUANTITY y $MAX_QUANTITY"))
        }

        return try {
            var result: Result<InvoiceData> = Result.failure(ChangeQuantityException("Error desconocido"))

            billingRepository.changeQuantity(
                transactionId = transactionId,
                itemPosId = itemPosId,
                lineNumber = lineNumber,
                newQuantity = newQuantity,
                partyAffiliationTypeCode = affiliateType,
                isAuthorized = true,
                authorizedBy = authorizedBy
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        Log.d(TAG, "Changing quantity...")
                    }
                    is Resource.Success -> {
                        val invoiceData = resource.data ?: InvoiceData()
                        Log.d(TAG, "Quantity changed successfully")
                        result = Result.success(invoiceData)
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Failed to change quantity: ${resource.message}")
                        result = Result.failure(ChangeQuantityException(resource.message ?: "Error al cambiar cantidad"))
                    }
                }
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Exception changing quantity: ${e.message}", e)
            Result.failure(ChangeQuantityException("Error: ${e.message}"))
        }
    }
}

/**
 * Exception thrown when change quantity fails
 */
class ChangeQuantityException(message: String) : Exception(message)
