package com.devlosoft.megaposmobile.presentation.billing

import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.devlosoft.megaposmobile.presentation.shared.components.AuthorizationDialogState

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
    val hasRecoverableTransaction: Boolean = false,

    // User permissions
    val userPermissions: UserPermissions? = null,

    // Authorization dialog state
    val authorizationDialogState: AuthorizationDialogState = AuthorizationDialogState(),
    val pendingAuthorizationAction: PendingAuthorizationAction? = null,

    // TODO dialog state (for unimplemented features)
    val showTodoDialog: Boolean = false,
    val todoDialogMessage: String = "",

    // Pause transaction state
    val showPauseConfirmDialog: Boolean = false,
    val isPausingTransaction: Boolean = false,
    val pauseTransactionError: String? = null,
    val shouldNavigateAfterPause: Boolean = false,

    // Print error state (for pause receipt)
    val showPrintErrorDialog: Boolean = false,
    val printErrorMessage: String? = null,
    val pendingPrintText: String? = null,
    val isPrinting: Boolean = false
)

/**
 * Represents an action that requires authorization
 */
sealed class PendingAuthorizationAction {
    data class DeleteLine(val itemId: String) : PendingAuthorizationAction()
    data class ChangeQuantity(val itemId: String) : PendingAuthorizationAction()
    data object AbortTransaction : PendingAuthorizationAction()
    data object PauseTransaction : PendingAuthorizationAction()
}
