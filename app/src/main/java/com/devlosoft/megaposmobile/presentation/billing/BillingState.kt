package com.devlosoft.megaposmobile.presentation.billing

import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData

data class BillingState(
    // Customer search state
    val customerSearchQuery: String = "",
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val isSearchingCustomer: Boolean = false,
    val customerSearchError: String? = null,

    // Transaction state
    val transactionCode: String = "",
    val isTransactionCreated: Boolean = false,
    val isCreatingTransaction: Boolean = false,
    val createTransactionError: String? = null,

    // Invoice/Items state
    val articleSearchQuery: String = "",
    val invoiceData: InvoiceData = InvoiceData(),
    val isAddingArticle: Boolean = false,
    val addArticleError: String? = null,

    // Finalize transaction state
    val isFinalizingTransaction: Boolean = false,
    val finalizeTransactionError: String? = null,
    val isTransactionFinalized: Boolean = false,

    // Navigation state
    val shouldNavigateToTransaction: Boolean = false,
    val shouldNavigateBackToBilling: Boolean = false,

    // Recovery check state
    val isCheckingRecovery: Boolean = false,
    val recoveryCheckError: String? = null,
    val hasRecoverableTransaction: Boolean = false
)
