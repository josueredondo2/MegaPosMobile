package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.data.remote.dto.DataphoneDataDto
import com.devlosoft.megaposmobile.data.remote.dto.PackagingItemDto
import com.devlosoft.megaposmobile.data.remote.dto.ValidateClientResponseDto
import com.devlosoft.megaposmobile.domain.model.AddMaterialResult
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.PackagingItem
import com.devlosoft.megaposmobile.domain.model.PrintDocument
import com.devlosoft.megaposmobile.domain.model.TodayTransaction
import com.devlosoft.megaposmobile.domain.model.TransactionRecoveryResult
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    suspend fun searchCustomer(identification: String): Flow<Resource<List<Customer>>>
    suspend fun addMaterial(
        transactionId: String,
        itemPosId: String,
        quantity: Double,
        partyAffiliationTypeCode: String?,
        sessionId: String? = null,
        workstationId: String? = null,
        customerId: String? = null,
        customerIdType: String? = null,
        customerName: String? = null,
        isAuthorized: Boolean = false,
        authorizedBy: String? = null,
        economicActivityId: String? = null,
        transactionTypeCode: String? = null
    ): Flow<Resource<AddMaterialResult>>
    suspend fun finalizeTransaction(
        sessionId: String,
        workstationId: String,
        transactionId: String,
        dataphoneData: DataphoneDataDto? = null
    ): Flow<Resource<Boolean>>
    suspend fun getPrintDocuments(
        transactionId: String,
        templateId: String = "01-FC",
        isReprint: Boolean = false,
        copyNumber: Int = 0
    ): Flow<Resource<List<PrintDocument>>>

    suspend fun canRecoverTransaction(
        sessionId: String,
        workstationId: String,
        transactionId: String? = null
    ): Flow<Resource<TransactionRecoveryResult>>

    suspend fun updateTransactionCustomer(
        transactionId: String,
        sessionId: String,
        workstationId: String,
        customerId: Int,
        customerIdType: String,
        customerName: String,
        affiliateType: String
    ): Flow<Resource<Boolean>>

    suspend fun updateTransaction(
        transactionId: String,
        sessionId: String,
        workstationId: String,
        customerId: Int? = null,
        customerIdType: String? = null,
        customerName: String? = null,
        affiliateType: String,
        transactionTypeCode: String? = null
    ): Flow<Resource<Boolean>>

    suspend fun getTransactionDetails(
        transactionId: String
    ): Flow<Resource<InvoiceData>>

    suspend fun pauseTransaction(
        transactionId: String,
        sessionId: String,
        workstationId: String
    ): Flow<Resource<Boolean>>

    suspend fun abortTransaction(
        sessionId: String,
        workstationId: String,
        transactionId: String,
        reason: String,
        authorizingOperator: String
    ): Flow<Resource<Boolean>>

    suspend fun voidItem(
        transactionId: String,
        itemPosId: String,
        authorizedOperator: String,
        affiliateType: String,
        deleteAll: Boolean
    ): Flow<Resource<Boolean>>

    suspend fun changeQuantity(
        transactionId: String,
        itemPosId: String,
        lineNumber: Int,
        newQuantity: Double,
        partyAffiliationTypeCode: String,
        isAuthorized: Boolean,
        authorizedBy: String?
    ): Flow<Resource<InvoiceData>>

    // Active transaction persistence methods
    suspend fun saveActiveTransactionId(transactionId: String)
    suspend fun getActiveTransactionId(): String?
    suspend fun clearActiveTransactionId()

    // Packaging methods
    suspend fun getPackagingReconciliation(
        transactionId: String
    ): Flow<Resource<List<PackagingItem>>>

    suspend fun updatePackagings(
        transactionId: String,
        packagings: List<PackagingItemDto>,
        affiliateType: String
    ): Flow<Resource<Boolean>>

    // Today's completed transactions
    suspend fun getTodayCompletedTransactions(workstationId: String): Flow<Resource<List<TodayTransaction>>>

    // FEL client validation
    suspend fun validateClientForFel(
        identificationType: String,
        identification: String,
        partyId: Int,
        documentType: String,
        userLogin: String
    ): Flow<Resource<ValidateClientResponseDto>>
}
