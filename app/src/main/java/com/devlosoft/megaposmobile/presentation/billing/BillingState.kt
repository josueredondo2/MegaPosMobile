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

    // Navigation state
    val shouldNavigateToTransaction: Boolean = false
)
