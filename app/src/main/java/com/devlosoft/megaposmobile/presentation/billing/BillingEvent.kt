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

    // Finalize transaction events
    data object FinalizeTransaction : BillingEvent()
    data object DismissFinalizeTransactionError : BillingEvent()
    data object ResetForNewTransaction : BillingEvent()

    // Navigation events
    data object GoBack : BillingEvent()

    // Recovery check events
    data object CheckTransactionRecovery : BillingEvent()
    data object DismissRecoveryCheckError : BillingEvent()

    // Authorization events
    data class RequestDeleteLine(val itemId: String, val itemName: String) : BillingEvent()
    data class RequestChangeQuantity(val itemId: String, val itemName: String) : BillingEvent()
    data object RequestAbortTransaction : BillingEvent()
    data object RequestPauseTransaction : BillingEvent()
    data class SubmitAuthorization(val userCode: String, val password: String) : BillingEvent()
    data object DismissAuthorizationDialog : BillingEvent()
    data object ClearAuthorizationError : BillingEvent()

    // TODO dialog events
    data class ShowTodoDialog(val message: String) : BillingEvent()
    data object DismissTodoDialog : BillingEvent()

    // Pause transaction events
    data object DismissPauseConfirmDialog : BillingEvent()
    data object ConfirmPauseTransaction : BillingEvent()
    data object DismissPauseTransactionError : BillingEvent()
    data object PauseNavigationHandled : BillingEvent()

    // Abort transaction events
    data object DismissAbortConfirmDialog : BillingEvent()
    data class AbortReasonChanged(val reason: String) : BillingEvent()
    data object ConfirmAbortTransaction : BillingEvent()
    data object DismissAbortTransactionError : BillingEvent()
    data object AbortNavigationHandled : BillingEvent()

    // Delete line events
    data object DismissDeleteLineError : BillingEvent()

    // Change quantity events
    data class ChangeQuantityValueChanged(val value: String) : BillingEvent()
    data object ConfirmChangeQuantity : BillingEvent()
    data object DismissChangeQuantityDialog : BillingEvent()
    data object DismissChangeQuantityError : BillingEvent()

    // Print error events (pause receipt)
    data object RetryPrint : BillingEvent()
    data object SkipPrint : BillingEvent()
    data object DismissPrintErrorDialog : BillingEvent()

    // Packaging events
    data object OpenPackagingDialog : BillingEvent()
    data object DismissPackagingDialog : BillingEvent()
    data class PackagingQuantityChanged(val itemPosId: String, val quantity: String) : BillingEvent()
    data object SubmitPackagings : BillingEvent()
    data object DismissPackagingsError : BillingEvent()
}
