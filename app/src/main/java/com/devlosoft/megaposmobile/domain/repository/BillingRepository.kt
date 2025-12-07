package com.devlosoft.megaposmobile.domain.repository

import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    suspend fun searchCustomer(identification: String): Flow<Resource<List<Customer>>>
    suspend fun createTransaction(
        sessionId: String,
        workstationId: String,
        customerId: String?,
        customerIdType: String?,
        customerName: String?
    ): Flow<Resource<String>>
    suspend fun addMaterial(
        transactionId: String,
        itemPosId: String,
        quantity: Double,
        partyAffiliationTypeCode: String?
    ): Flow<Resource<InvoiceData>>
}
