package com.devlosoft.megaposmobile.presentation.billing

import com.devlosoft.megaposmobile.data.remote.dto.EconomicActivityDto
import com.devlosoft.megaposmobile.data.remote.dto.EconomicActivitySearchItemDto
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.devlosoft.megaposmobile.presentation.billing.state.CatalogDialogState
import com.devlosoft.megaposmobile.presentation.billing.state.PackagingDialogState
import com.devlosoft.megaposmobile.presentation.billing.state.PrintState
import com.devlosoft.megaposmobile.presentation.billing.state.TransactionControlState
import com.devlosoft.megaposmobile.presentation.shared.components.AuthorizationDialogState

data class BillingState(
    // Customer search state
    val customerSearchQuery: String = "",
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val isSearchingCustomer: Boolean = false,
    val customerSearchError: String? = null,
    val documentType: String = "CO",  // "CO" = Tiquete Electronico (default), "FC" = Factura Electronica

    // FEL client validation state
    val isValidatingClient: Boolean = false,
    val clientValidationError: String? = null,

    // Economic activity selection state
    val showActivityDialog: Boolean = false,
    val economicActivities: List<EconomicActivityDto> = emptyList(),  // From client validation
    val activitySearchQuery: String = "",
    val selectedActivity: EconomicActivityDto? = null,
    // Search results state
    val searchedActivities: List<EconomicActivitySearchItemDto> = emptyList(),
    val selectedSearchActivity: EconomicActivitySearchItemDto? = null,
    val isSearchingActivities: Boolean = false,
    val activitySearchError: String? = null,
    val activityCurrentPage: Int = 1,
    val activityHasNextPage: Boolean = false,
    val isLoadingMoreActivities: Boolean = false,
    val selectedEconomicActivityCode: String? = null,  // Economic activity code for addMaterial

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
    val hasRecoverableTransaction: Boolean = false,

    // User permissions
    val userPermissions: UserPermissions? = null,

    // Authorization dialog state
    val authorizationDialogState: AuthorizationDialogState = AuthorizationDialogState(),

    // TODO dialog state (for unimplemented features)
    val showTodoDialog: Boolean = false,
    val todoDialogMessage: String = "",

    // Transaction control state (pause/abort) - using sub-state
    val transactionControl: TransactionControlState = TransactionControlState(),

    // Delete line state
    val isDeletingLine: Boolean = false,
    val deleteLineError: String? = null,

    // Change quantity state
    val showChangeQuantityDialog: Boolean = false,
    val changeQuantityItemId: String = "",
    val changeQuantityItemName: String = "",
    val changeQuantityLineNumber: Int = 0,
    val changeQuantityCurrentQty: Double = 0.0,
    val changeQuantityNewQty: String = "",
    val changeQuantityAuthorizedBy: String? = null,
    val isChangingQuantity: Boolean = false,
    val changeQuantityError: String? = null,

    // Print state - using sub-state
    val printState: PrintState = PrintState(),

    // Packaging dialog state - using sub-state
    val packagingState: PackagingDialogState = PackagingDialogState(),

    // Catalog dialog state - using sub-state
    val catalogState: CatalogDialogState = CatalogDialogState()
) {
    // Convenience accessors for backward compatibility with UI
    // Transaction control
    val showPauseConfirmDialog: Boolean get() = transactionControl.showPauseConfirmDialog
    val isPausingTransaction: Boolean get() = transactionControl.isPausingTransaction
    val pauseTransactionError: String? get() = transactionControl.pauseTransactionError
    val shouldNavigateAfterPause: Boolean get() = transactionControl.shouldNavigateAfterPause
    val showAbortConfirmDialog: Boolean get() = transactionControl.showAbortConfirmDialog
    val abortReason: String get() = transactionControl.abortReason
    val abortAuthorizingOperator: String get() = transactionControl.abortAuthorizingOperator
    val isAbortingTransaction: Boolean get() = transactionControl.isAbortingTransaction
    val abortTransactionError: String? get() = transactionControl.abortTransactionError
    val shouldNavigateAfterAbort: Boolean get() = transactionControl.shouldNavigateAfterAbort

    // Print state
    val showPrintErrorDialog: Boolean get() = printState.showPrintErrorDialog
    val printErrorMessage: String? get() = printState.printErrorMessage
    val pendingPrintText: String? get() = printState.pendingPrintText
    val pendingPrintTransactionCode: String? get() = printState.pendingPrintTransactionCode
    val isPrinting: Boolean get() = printState.isPrinting

    // Packaging state
    val showPackagingDialog: Boolean get() = packagingState.isVisible
    val packagingItems: List<com.devlosoft.megaposmobile.domain.model.PackagingItem> get() = packagingState.items
    val packagingInputs: Map<String, String> get() = packagingState.inputs
    val isLoadingPackagings: Boolean get() = packagingState.isLoading
    val loadPackagingsError: String? get() = packagingState.loadError
    val isUpdatingPackagings: Boolean get() = packagingState.isUpdating
    val updatePackagingsError: String? get() = packagingState.updateError
}
