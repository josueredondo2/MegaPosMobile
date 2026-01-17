package com.devlosoft.megaposmobile.presentation.billing

import com.devlosoft.megaposmobile.data.remote.dto.EconomicActivityDto
import com.devlosoft.megaposmobile.data.remote.dto.EconomicActivitySearchItemDto
import com.devlosoft.megaposmobile.domain.model.Customer

sealed class BillingEvent {
    // Customer search events
    data class CustomerSearchQueryChanged(val query: String) : BillingEvent()
    data object SearchCustomer : BillingEvent()
    data class SelectCustomer(val customer: Customer) : BillingEvent()
    data object ClearCustomerSearch : BillingEvent()
    data object DismissCustomerSearchError : BillingEvent()
    data class DocumentTypeChanged(val type: String) : BillingEvent()
    data object DismissClientValidationError : BillingEvent()

    // Economic activity selection events
    data class ActivitySearchQueryChanged(val query: String) : BillingEvent()
    data class SelectActivity(val activity: EconomicActivityDto) : BillingEvent()
    data class SelectSearchActivity(val activity: EconomicActivitySearchItemDto) : BillingEvent()
    data object SearchActivities : BillingEvent()  // Triggered on Enter/OK
    data object LoadMoreActivities : BillingEvent()  // Load next page
    data object ConfirmActivitySelection : BillingEvent()
    data object DismissActivityDialog : BillingEvent()

    // Transaction events
    data object StartTransaction : BillingEvent()
    data object DismissCreateTransactionError : BillingEvent()
    data object NavigationHandled : BillingEvent()

    // Session validation events
    data object NavigateToLogin : BillingEvent()
    data object DismissSessionExpiredError : BillingEvent()

    // Article events
    data class ArticleSearchQueryChanged(val query: String) : BillingEvent()
    data object AddArticle : BillingEvent()
    data object DismissAddArticleError : BillingEvent()

    // Scanner events (Zebra/PAX hardware scanner)
    data class ScannerInput(val barcode: String) : BillingEvent()

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

    // Catalog events
    data object OpenCatalogDialog : BillingEvent()
    data object DismissCatalogDialog : BillingEvent()
    data class SelectCatalogCategory(val catalogTypeId: Int) : BillingEvent()
    data class SelectCatalogLetter(val letter: Char) : BillingEvent()
    data class AddCatalogItem(val itemPosId: String) : BillingEvent()
    data object DismissCatalogError : BillingEvent()
}
