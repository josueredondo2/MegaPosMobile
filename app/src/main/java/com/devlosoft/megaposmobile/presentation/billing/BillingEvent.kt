package com.devlosoft.megaposmobile.presentation.billing

import com.devlosoft.megaposmobile.domain.model.Customer

sealed class BillingEvent {
    // Customer search events
    data class CustomerSearchQueryChanged(val query: String) : BillingEvent()
    data object SearchCustomer : BillingEvent()
    data class SelectCustomer(val customer: Customer) : BillingEvent()
    data object ClearCustomerSearch : BillingEvent()
    data object DismissCustomerSearchError : BillingEvent()

    // Transaction events
    data object StartTransaction : BillingEvent()
    data object DismissCreateTransactionError : BillingEvent()
    data object NavigationHandled : BillingEvent()

    // Article events
    data class ArticleSearchQueryChanged(val query: String) : BillingEvent()
    data object AddArticle : BillingEvent()
    data object DismissAddArticleError : BillingEvent()

    // Navigation events
    data object GoBack : BillingEvent()
}
