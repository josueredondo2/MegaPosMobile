package com.devlosoft.megaposmobile.presentation.billing

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.printer.LocalPrintTemplates
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.data.remote.dto.PackagingItemDto
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.InvoiceItem
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import com.devlosoft.megaposmobile.domain.usecase.AuthorizeProcessUseCase
import com.devlosoft.megaposmobile.domain.usecase.GetSessionInfoUseCase
import com.devlosoft.megaposmobile.domain.usecase.PrintDocumentsUseCase
import com.devlosoft.megaposmobile.domain.usecase.PrinterFailureException
import com.devlosoft.megaposmobile.domain.usecase.billing.AbortTransactionUseCase
import com.devlosoft.megaposmobile.domain.usecase.billing.AuthorizedActionHandler
import com.devlosoft.megaposmobile.domain.usecase.billing.ChangeQuantityUseCase
import com.devlosoft.megaposmobile.domain.usecase.billing.GetPackagingReconciliationUseCase
import com.devlosoft.megaposmobile.domain.usecase.billing.PauseTransactionUseCase
import com.devlosoft.megaposmobile.domain.usecase.billing.PrintPauseReceiptUseCase
import com.devlosoft.megaposmobile.domain.usecase.billing.UpdatePackagingsUseCase
import com.devlosoft.megaposmobile.domain.usecase.billing.VoidItemUseCase
import com.devlosoft.megaposmobile.core.extensions.getVisibleItems
import com.devlosoft.megaposmobile.core.extensions.isPackagingItem
import com.devlosoft.megaposmobile.core.extensions.getTotalItemCount
import com.devlosoft.megaposmobile.presentation.billing.state.PackagingDialogState
import com.devlosoft.megaposmobile.presentation.billing.state.PrintState
import com.devlosoft.megaposmobile.presentation.billing.state.TransactionControlState
import com.devlosoft.megaposmobile.presentation.shared.components.AuthorizationDialogState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val sessionManager: SessionManager,
    private val authorizeProcessUseCase: AuthorizeProcessUseCase,
    private val printDocumentsUseCase: PrintDocumentsUseCase,
    private val getSessionInfoUseCase: GetSessionInfoUseCase,
    private val printerManager: PrinterManager,
    private val authorizedActionHandler: AuthorizedActionHandler,
    private val pauseTransactionUseCase: PauseTransactionUseCase,
    private val abortTransactionUseCase: AbortTransactionUseCase,
    private val getPackagingReconciliationUseCase: GetPackagingReconciliationUseCase,
    private val updatePackagingsUseCase: UpdatePackagingsUseCase,
    private val printPauseReceiptUseCase: PrintPauseReceiptUseCase,
    private val voidItemUseCase: VoidItemUseCase,
    private val changeQuantityUseCase: ChangeQuantityUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "BillingViewModel"
    }

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    // Callback for pending authorized action - replaces PendingAuthorizationAction pattern
    private var pendingAuthorizedAction: (suspend (authorizedBy: String) -> Unit)? = null
    private var currentProcessCode: String = ""

    // Check if we should skip recovery check (coming from completed transaction)
    private val skipRecoveryCheck: Boolean = savedStateHandle.get<Boolean>("skipRecoveryCheck") ?: false

    init {
        loadUserPermissions()
        // Only check for transaction recovery if not skipped
        if (!skipRecoveryCheck) {
            checkTransactionRecovery()
        }
    }

    private fun loadUserPermissions() {
        viewModelScope.launch {
            val permissions = sessionManager.getUserPermissionsSync()
            _state.update { it.copy(userPermissions = permissions) }
        }
    }

    fun onEvent(event: BillingEvent) {
        when (event) {
            is BillingEvent.CustomerSearchQueryChanged -> {
                _state.update { it.copy(customerSearchQuery = event.query) }
            }
            is BillingEvent.SearchCustomer -> {
                searchCustomer()
            }
            is BillingEvent.SelectCustomer -> {
                selectCustomer(event.customer)
            }
            is BillingEvent.ClearCustomerSearch -> {
                _state.update {
                    it.copy(
                        customerSearchQuery = "",
                        customers = emptyList(),
                        selectedCustomer = null
                    )
                }
            }
            is BillingEvent.DismissCustomerSearchError -> {
                _state.update { it.copy(customerSearchError = null) }
            }
            is BillingEvent.StartTransaction -> {
                startTransaction()
            }
            is BillingEvent.DismissCreateTransactionError -> {
                _state.update { it.copy(createTransactionError = null) }
            }
            is BillingEvent.NavigationHandled -> {
                _state.update { it.copy(shouldNavigateToTransaction = false) }
            }
            is BillingEvent.ArticleSearchQueryChanged -> {
                _state.update { it.copy(articleSearchQuery = event.query) }
            }
            is BillingEvent.AddArticle -> {
                addArticle()
            }
            is BillingEvent.ScannerInput -> {
                // Set barcode from scanner and add article automatically
                _state.update { it.copy(articleSearchQuery = event.barcode) }
                addArticle()
            }
            is BillingEvent.DismissAddArticleError -> {
                _state.update { it.copy(addArticleError = null) }
            }
            is BillingEvent.FinalizeTransaction -> {
                finalizeTransaction()
            }
            is BillingEvent.DismissFinalizeTransactionError -> {
                _state.update { it.copy(finalizeTransactionError = null) }
            }
            is BillingEvent.ResetForNewTransaction -> {
                // Reset all state for a new transaction
                _state.value = BillingState()
            }
            is BillingEvent.GoBack -> {
                // Handle in screen
            }
            is BillingEvent.CheckTransactionRecovery -> {
                checkTransactionRecovery()
            }
            is BillingEvent.DismissRecoveryCheckError -> {
                _state.update { it.copy(recoveryCheckError = null) }
            }
            // Authorization events
            is BillingEvent.RequestDeleteLine -> {
                handleDeleteLineRequest(event.itemId, event.itemName)
            }
            is BillingEvent.RequestChangeQuantity -> {
                handleChangeQuantityRequest(event.itemId, event.itemName)
            }
            is BillingEvent.RequestAbortTransaction -> {
                handleAbortTransactionRequest()
            }
            is BillingEvent.RequestPauseTransaction -> {
                handlePauseTransactionRequest()
            }
            is BillingEvent.SubmitAuthorization -> {
                submitAuthorization(event.userCode, event.password)
            }
            is BillingEvent.DismissAuthorizationDialog -> {
                pendingAuthorizedAction = null
                currentProcessCode = ""
                _state.update {
                    it.copy(authorizationDialogState = AuthorizationDialogState())
                }
            }
            is BillingEvent.ClearAuthorizationError -> {
                _state.update {
                    it.copy(
                        authorizationDialogState = it.authorizationDialogState.copy(error = null)
                    )
                }
            }
            is BillingEvent.ShowTodoDialog -> {
                _state.update { it.copy(showTodoDialog = true, todoDialogMessage = event.message) }
            }
            is BillingEvent.DismissTodoDialog -> {
                _state.update { it.copy(showTodoDialog = false, todoDialogMessage = "") }
            }
            // Pause transaction events
            is BillingEvent.DismissPauseConfirmDialog -> {
                _state.update {
                    it.copy(transactionControl = it.transactionControl.copy(showPauseConfirmDialog = false))
                }
            }
            is BillingEvent.ConfirmPauseTransaction -> {
                confirmPauseTransaction()
            }
            is BillingEvent.DismissPauseTransactionError -> {
                _state.update {
                    it.copy(transactionControl = it.transactionControl.copy(pauseTransactionError = null))
                }
            }
            is BillingEvent.PauseNavigationHandled -> {
                _state.update {
                    it.copy(transactionControl = it.transactionControl.copy(shouldNavigateAfterPause = false))
                }
            }
            // Abort transaction events
            is BillingEvent.DismissAbortConfirmDialog -> {
                _state.update {
                    it.copy(transactionControl = it.transactionControl.copy(
                        showAbortConfirmDialog = false,
                        abortReason = "",
                        abortAuthorizingOperator = ""
                    ))
                }
            }
            is BillingEvent.AbortReasonChanged -> {
                _state.update {
                    it.copy(transactionControl = it.transactionControl.copy(abortReason = event.reason))
                }
            }
            is BillingEvent.ConfirmAbortTransaction -> {
                confirmAbortTransaction()
            }
            is BillingEvent.DismissAbortTransactionError -> {
                _state.update {
                    it.copy(transactionControl = it.transactionControl.copy(abortTransactionError = null))
                }
            }
            is BillingEvent.AbortNavigationHandled -> {
                _state.update {
                    it.copy(transactionControl = it.transactionControl.copy(shouldNavigateAfterAbort = false))
                }
            }
            // Delete line events
            is BillingEvent.DismissDeleteLineError -> {
                _state.update { it.copy(deleteLineError = null) }
            }
            // Change quantity events
            is BillingEvent.ChangeQuantityValueChanged -> {
                _state.update { it.copy(changeQuantityNewQty = event.value) }
            }
            is BillingEvent.ConfirmChangeQuantity -> {
                confirmChangeQuantity()
            }
            is BillingEvent.DismissChangeQuantityDialog -> {
                _state.update {
                    it.copy(
                        showChangeQuantityDialog = false,
                        changeQuantityNewQty = "",
                        changeQuantityItemId = "",
                        changeQuantityItemName = "",
                        changeQuantityLineNumber = 0,
                        changeQuantityCurrentQty = 0.0
                    )
                }
            }
            is BillingEvent.DismissChangeQuantityError -> {
                _state.update { it.copy(changeQuantityError = null) }
            }
            // Print error events
            is BillingEvent.RetryPrint -> {
                retryPrint()
            }
            is BillingEvent.SkipPrint -> {
                skipPrint()
            }
            is BillingEvent.DismissPrintErrorDialog -> {
                _state.update {
                    it.copy(
                        printState = it.printState.copy(
                            showPrintErrorDialog = false,
                            printErrorMessage = null
                        )
                    )
                }
            }
            // Packaging events
            is BillingEvent.OpenPackagingDialog -> {
                openPackagingDialog()
            }
            is BillingEvent.DismissPackagingDialog -> {
                _state.update {
                    it.copy(packagingState = PackagingDialogState())
                }
            }
            is BillingEvent.PackagingQuantityChanged -> {
                _state.update {
                    it.copy(
                        packagingState = it.packagingState.copy(
                            inputs = it.packagingState.inputs + (event.itemPosId to event.quantity)
                        )
                    )
                }
            }
            is BillingEvent.SubmitPackagings -> {
                submitPackagings()
            }
            is BillingEvent.DismissPackagingsError -> {
                _state.update {
                    it.copy(
                        packagingState = it.packagingState.copy(
                            loadError = null,
                            updateError = null
                        )
                    )
                }
            }
        }
    }

    private fun checkTransactionRecovery() {
        viewModelScope.launch {
            val sessionId = sessionManager.getSessionId().first()
            val stationId = sessionManager.getStationId().first()

            if (sessionId.isNullOrBlank() || stationId.isNullOrBlank()) {
                // No session, allow new transaction
                return@launch
            }

            // Get saved transactionId from local database
            val savedTransactionId = billingRepository.getActiveTransactionId()

            billingRepository.canRecoverTransaction(sessionId, stationId, savedTransactionId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isCheckingRecovery = true) }
                    }
                    is Resource.Success -> {
                        val recovery = result.data!!
                        if (recovery.canRecover && !recovery.canCreate) {
                            // Recover transaction - populate state and navigate
                            _state.update {
                                it.copy(
                                    isCheckingRecovery = false,
                                    transactionCode = recovery.transactionId ?: "",
                                    isTransactionCreated = true,
                                    invoiceData = recovery.invoiceData ?: InvoiceData(),
                                    selectedCustomer = recovery.customer,
                                    hasRecoverableTransaction = true,
                                    shouldNavigateToTransaction = true
                                )
                            }
                        } else {
                            // Can create new - normal flow
                            // Clear local record if can't recover
                            if (savedTransactionId != null) {
                                billingRepository.clearActiveTransactionId()
                            }
                            _state.update {
                                it.copy(isCheckingRecovery = false)
                            }
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isCheckingRecovery = false,
                                recoveryCheckError = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun selectCustomer(customer: Customer) {
        _state.update { it.copy(selectedCustomer = customer) }

        val transactionCode = _state.value.transactionCode
        if (transactionCode.isNotBlank()) {
            updateTransactionCustomer(customer)
        }
    }

    private fun updateTransactionCustomer(customer: Customer) {
        viewModelScope.launch {
            val sessionId = sessionManager.getSessionId().first()
            val stationId = sessionManager.getStationId().first()
            val transactionCode = _state.value.transactionCode

            if (sessionId.isNullOrBlank() || stationId.isNullOrBlank() || transactionCode.isBlank()) {
                return@launch
            }

            billingRepository.updateTransactionCustomer(
                transactionId = transactionCode,
                sessionId = sessionId,
                workstationId = stationId,
                customerId = customer.partyId,
                customerIdType = customer.identificationType,
                customerName = customer.name,
                affiliateType = customer.affiliateType
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                    }
                    is Resource.Success -> {
                        Log.d(TAG, "Cliente actualizado exitosamente en la transacción")
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error al actualizar cliente: ${result.message}")
                        // Opcionalmente mostrar error al usuario
                    }
                }
            }
        }
    }

    private fun searchCustomer() {
        val query = _state.value.customerSearchQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            billingRepository.searchCustomer(query).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update {
                            it.copy(
                                isSearchingCustomer = true,
                                customerSearchError = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        val customers = result.data ?: emptyList()
                        val selectedCustomer = if (customers.size == 1) customers.first() else null

                        _state.update {
                            it.copy(
                                isSearchingCustomer = false,
                                customers = customers,
                                selectedCustomer = selectedCustomer
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isSearchingCustomer = false,
                                customers = emptyList(),
                                selectedCustomer = null,
                                customerSearchError = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startTransaction() {
        // Use default customer if none selected
        val customerToUse = _state.value.selectedCustomer ?: Customer.DEFAULT
        val transactionCode = _state.value.transactionCode

        if (transactionCode.isNotBlank()) {
            viewModelScope.launch {
                val sessionId = sessionManager.getSessionId().first()
                val stationId = sessionManager.getStationId().first()

                if (sessionId.isNullOrBlank() || stationId.isNullOrBlank()) {
                    _state.update {
                        it.copy(createTransactionError = "No hay sesión activa")
                    }
                    return@launch
                }

                _state.update { it.copy(isCreatingTransaction = true) }

                billingRepository.updateTransactionCustomer(
                    transactionId = transactionCode,
                    sessionId = sessionId,
                    workstationId = stationId,
                    customerId = customerToUse.partyId,
                    customerIdType = customerToUse.identificationType,
                    customerName = customerToUse.name,
                    affiliateType = customerToUse.affiliateType
                ).collect { result ->
                    when (result) {
                        is Resource.Loading -> { }
                        is Resource.Success -> {
                            getTransactionDetailsAndNavigate(transactionCode, customerToUse)
                        }
                        is Resource.Error -> {
                            _state.update {
                                it.copy(
                                    isCreatingTransaction = false,
                                    createTransactionError = result.message
                                )
                            }
                        }
                    }
                }
            }
        } else {
            _state.update {
                it.copy(
                    selectedCustomer = customerToUse,
                    shouldNavigateToTransaction = true
                )
            }
        }
    }

    private fun loadTransactionDetails(transactionCode: String) {
        viewModelScope.launch {
            billingRepository.getTransactionDetails(transactionCode).collect { result ->
                when (result) {
                    is Resource.Loading -> { }
                    is Resource.Success -> {
                        _state.update {
                            it.copy(invoiceData = result.data ?: InvoiceData())
                        }
                    }
                    is Resource.Error -> {
                        Log.e(TAG, "Error loading transaction details: ${result.message}")
                    }
                }
            }
        }
    }

    private fun getTransactionDetailsAndNavigate(transactionCode: String, customer: Customer) {
        viewModelScope.launch {
            billingRepository.getTransactionDetails(transactionCode).collect { result ->
                when (result) {
                    is Resource.Loading -> { }
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                isCreatingTransaction = false,
                                selectedCustomer = customer,
                                invoiceData = result.data ?: InvoiceData(),
                                shouldNavigateToTransaction = true
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isCreatingTransaction = false,
                                selectedCustomer = customer,
                                shouldNavigateToTransaction = true
                            )
                        }
                    }
                }
            }
        }
    }

    private fun addArticle() {
        val articleId = _state.value.articleSearchQuery.trim()
        if (articleId.isBlank()) return

        viewModelScope.launch {
            val currentTransactionCode = _state.value.transactionCode
            val selectedCustomer = _state.value.selectedCustomer

            // create new transaction on new item
            var sessionId: String? = null
            var workstationId: String? = null

            if (currentTransactionCode.isBlank()) {
                sessionId = sessionManager.getSessionId().first()
                workstationId = sessionManager.getStationId().first()

                if (sessionId.isNullOrBlank() || workstationId.isNullOrBlank()) {
                    _state.update {
                        it.copy(addArticleError = "No hay sesión activa")
                    }
                    return@launch
                }
            }

            val customerId = if (currentTransactionCode.isBlank()) selectedCustomer?.partyId?.toString() else null
            val customerIdType = if (currentTransactionCode.isBlank()) selectedCustomer?.identificationType else null
            val customerName = if (currentTransactionCode.isBlank()) selectedCustomer?.name else null

            // Check if current user has permission to authorize restricted materials
            val permissions = _state.value.userPermissions
            val canAuthorizeRestrictedMaterials = permissions?.hasAccess(UserPermissions.PROCESS_AUTORIZAR_MATERIAL_RESTRINGIDO) ?: false
            val userCode = if (canAuthorizeRestrictedMaterials) sessionManager.getUserCode().first() else null

            billingRepository.addMaterial(
                transactionId = currentTransactionCode,
                itemPosId = articleId,
                quantity = 1.0,
                partyAffiliationTypeCode = selectedCustomer?.affiliateType,
                sessionId = sessionId,
                workstationId = workstationId,
                customerId = customerId,
                customerIdType = customerIdType,
                customerName = customerName,
                isAuthorized = canAuthorizeRestrictedMaterials,
                authorizedBy = userCode
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update {
                            it.copy(
                                isAddingArticle = true,
                                addArticleError = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        val addResult = result.data!!
                        // Save transactionId to local database if new transaction was created
                        if (addResult.transactionId != null && _state.value.transactionCode.isBlank()) {
                            billingRepository.saveActiveTransactionId(addResult.transactionId)
                        }
                        _state.update {
                            it.copy(
                                isAddingArticle = false,
                                // Actualizar transactionCode si se creó una nueva transacción
                                transactionCode = addResult.transactionId ?: it.transactionCode,
                                isTransactionCreated = addResult.transactionId != null || it.isTransactionCreated,
                                invoiceData = addResult.invoiceData,
                                articleSearchQuery = "" // Limpiar el input
                            )
                        }
                    }
                    is Resource.Error -> {
                        if (result.errorCode == "ITEM_REQUIRES_AUTHORIZATION") {
                            handleMaterialAuthorizationRequest(
                                itemPosId = articleId,
                                quantity = 1.0,
                                partyAffiliationTypeCode = selectedCustomer?.affiliateType
                            )
                        } else {
                            _state.update {
                                it.copy(
                                    isAddingArticle = false,
                                    addArticleError = result.message
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleMaterialAuthorizationRequest(
        itemPosId: String,
        quantity: Double,
        partyAffiliationTypeCode: String?
    ) {
        // Set callback for material authorization
        pendingAuthorizedAction = { authorizedBy ->
            executeAddMaterialWithAuthorization(
                itemPosId = itemPosId,
                quantity = quantity,
                partyAffiliationTypeCode = partyAffiliationTypeCode,
                authorizedBy = authorizedBy
            )
        }
        currentProcessCode = UserPermissions.PROCESS_AUTORIZAR_MATERIAL_RESTRINGIDO

        _state.update {
            it.copy(
                isAddingArticle = false,
                articleSearchQuery = "", // Limpiar el input
                authorizationDialogState = AuthorizationDialogState(
                    isVisible = true,
                    title = "Artículo Restringido",
                    message = "Este artículo requiere autorización para ser vendido",
                    actionButtonText = "Autorizar",
                    processCode = UserPermissions.PROCESS_AUTORIZAR_MATERIAL_RESTRINGIDO
                )
            )
        }
    }

    private fun finalizeTransaction() {
        Log.d(TAG, "finalizeTransaction() called")
        val transactionCode = _state.value.transactionCode
        if (transactionCode.isBlank()) {
            Log.e(TAG, "No transaction code")
            _state.update {
                it.copy(finalizeTransactionError = "No hay transacción activa")
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Getting session data...")
                val sessionId = sessionManager.getSessionId().first()
                val stationId = sessionManager.getStationId().first()
                Log.d(TAG, "Session: $sessionId, Station: $stationId")

                if (sessionId.isNullOrBlank() || stationId.isNullOrBlank()) {
                    _state.update {
                        it.copy(finalizeTransactionError = "No hay sesión activa")
                    }
                    return@launch
                }

                Log.d(TAG, "Calling billingRepository.finalizeTransaction...")
                billingRepository.finalizeTransaction(
                    sessionId = sessionId,
                    workstationId = stationId,
                    transactionId = transactionCode
                ).collect { result ->
                    Log.d(TAG, "Result received: $result")
                    when (result) {
                        is Resource.Loading -> {
                            Log.d(TAG, "Loading...")
                            _state.update {
                                it.copy(
                                    isFinalizingTransaction = true,
                                    finalizeTransactionError = null
                                )
                            }
                        }
                        is Resource.Success -> {
                            Log.d(TAG, "Success! Now fetching print documents...")
                            // Clear active transactionId from local database
                            billingRepository.clearActiveTransactionId()
                            // Transaction finalized successfully, now fetch print documents
                            fetchAndPrintDocuments(transactionCode)
                        }
                        is Resource.Error -> {
                            Log.e(TAG, "Error: ${result.message}")
                            _state.update {
                                it.copy(
                                    isFinalizingTransaction = false,
                                    finalizeTransactionError = result.message
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in finalizeTransaction: ${e.message}", e)
                _state.update {
                    it.copy(
                        isFinalizingTransaction = false,
                        finalizeTransactionError = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun fetchAndPrintDocuments(transactionCode: String) {
        Log.d(TAG, "fetchAndPrintDocuments() called for transaction: $transactionCode")

        _state.update {
            it.copy(
                isFinalizingTransaction = false,
                printState = it.printState.copy(
                    isPrinting = true,
                    pendingPrintTransactionCode = transactionCode
                )
            )
        }

        printDocumentsUseCase(transactionCode)
            .onSuccess { printedCount ->
                Log.d(TAG, "Successfully printed $printedCount documents")
                _state.update {
                    it.copy(
                        printState = PrintState(),
                        isTransactionFinalized = true,
                        shouldNavigateBackToBilling = true
                    )
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Error printing documents: ${error.message}")
                // Check if documents were retrieved before print failed
                val docsRetrieved = error is PrinterFailureException
                // Show print error dialog with retry/skip options
                _state.update {
                    it.copy(
                        printState = it.printState.copy(
                            isPrinting = false,
                            showPrintErrorDialog = true,
                            printErrorMessage = error.message ?: "Error al imprimir los documentos",
                            pendingPrintTransactionCode = transactionCode,
                            documentsRetrievedBeforeFail = docsRetrieved
                        ),
                        isTransactionFinalized = true // Transaction is finalized, only print failed
                    )
                }
            }
    }

    // Authorization methods

    private fun handleDeleteLineRequest(itemId: String, itemName: String) {
        val allItems = _state.value.invoiceData.items

        // Validation 1: Don't allow deleting packaging items (children) directly
        if (allItems.isPackagingItem(itemId)) {
            _state.update {
                it.copy(
                    deleteLineError = "No se puede eliminar un envase. Elimine el artículo principal."
                )
            }
            return
        }

        // Validation 2: Count "visible" items (excluding deleted AND orphaned packaging)
        if (allItems.getVisibleItems().size <= 1) {
            _state.update {
                it.copy(
                    deleteLineError = "No se puede eliminar la única línea de la factura"
                )
            }
            return
        }

        viewModelScope.launch {
            when (val result = authorizedActionHandler.checkAccess(
                _state.value.userPermissions,
                UserPermissions.PROCESS_ELIMINAR_LINEA
            )) {
                is AuthorizedActionHandler.CheckResult.HasAccess -> {
                    executeDeleteLine(itemId, result.userCode)
                }
                is AuthorizedActionHandler.CheckResult.RequiresAuthorization -> {
                    pendingAuthorizedAction = { authorizedBy -> executeDeleteLine(itemId, authorizedBy) }
                    currentProcessCode = UserPermissions.PROCESS_ELIMINAR_LINEA
                    _state.update {
                        it.copy(
                            authorizationDialogState = AuthorizationDialogState(
                                isVisible = true,
                                title = "Eliminar Línea",
                                message = "Para eliminar el articulo $itemName debe solicitar autorización",
                                actionButtonText = "Eliminar Línea",
                                processCode = UserPermissions.PROCESS_ELIMINAR_LINEA
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handleChangeQuantityRequest(itemId: String, itemName: String) {
        // Find the item to get lineNumber and current quantity
        val item = _state.value.invoiceData.items.find { it.itemId == itemId }
        if (item == null) {
            Log.e(TAG, "Item not found: $itemId")
            return
        }

        viewModelScope.launch {
            when (val result = authorizedActionHandler.checkAccess(
                _state.value.userPermissions,
                UserPermissions.PROCESS_CAMBIAR_CANTIDAD_ARTICULO
            )) {
                is AuthorizedActionHandler.CheckResult.HasAccess -> {
                    // User has access, show quantity dialog directly with user's code as authorizer
                    showChangeQuantityDialog(itemId, itemName, item.lineItemSequence, item.quantity, result.userCode)
                }
                is AuthorizedActionHandler.CheckResult.RequiresAuthorization -> {
                    // Set callback and show authorization dialog
                    pendingAuthorizedAction = { authorizedBy -> executeChangeQuantity(itemId, authorizedBy) }
                    currentProcessCode = UserPermissions.PROCESS_CAMBIAR_CANTIDAD_ARTICULO
                    // Store item info in state for after authorization
                    _state.update {
                        it.copy(
                            changeQuantityItemId = itemId,
                            changeQuantityItemName = itemName,
                            changeQuantityLineNumber = item.lineItemSequence,
                            changeQuantityCurrentQty = item.quantity,
                            changeQuantityAuthorizedBy = null,
                            authorizationDialogState = AuthorizationDialogState(
                                isVisible = true,
                                title = "Cambiar Cantidad",
                                message = "Para cambiar la cantidad del articulo $itemName debe solicitar autorización",
                                actionButtonText = "Cambiar Cantidad",
                                processCode = UserPermissions.PROCESS_CAMBIAR_CANTIDAD_ARTICULO
                            )
                        )
                    }
                }
            }
        }
    }

    private fun showChangeQuantityDialog(itemId: String, itemName: String, lineNumber: Int, currentQty: Double, authorizedBy: String?) {
        _state.update {
            it.copy(
                showChangeQuantityDialog = true,
                changeQuantityItemId = itemId,
                changeQuantityItemName = itemName,
                changeQuantityLineNumber = lineNumber,
                changeQuantityCurrentQty = currentQty,
                changeQuantityNewQty = currentQty.toInt().toString(), // Pre-fill with current quantity
                changeQuantityAuthorizedBy = authorizedBy
            )
        }
    }

    private fun handleAbortTransactionRequest() {
        viewModelScope.launch {
            when (val result = authorizedActionHandler.checkAccess(
                _state.value.userPermissions,
                UserPermissions.PROCESS_ABORTAR_TRANSACCION
            )) {
                is AuthorizedActionHandler.CheckResult.HasAccess -> {
                    // User has access, execute action directly
                    executeAbortTransaction(result.userCode)
                }
                is AuthorizedActionHandler.CheckResult.RequiresAuthorization -> {
                    // Set callback and show authorization dialog
                    pendingAuthorizedAction = { authorizedBy -> executeAbortTransaction(authorizedBy) }
                    currentProcessCode = UserPermissions.PROCESS_ABORTAR_TRANSACCION
                    _state.update {
                        it.copy(
                            authorizationDialogState = AuthorizationDialogState(
                                isVisible = true,
                                title = "Abortar Transacción",
                                message = "Para abortar la transacción debe solicitar autorización",
                                actionButtonText = "Abortar Transacción",
                                processCode = UserPermissions.PROCESS_ABORTAR_TRANSACCION
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handlePauseTransactionRequest() {
        viewModelScope.launch {
            when (val result = authorizedActionHandler.checkAccess(
                _state.value.userPermissions,
                UserPermissions.PROCESS_TRANSACCION_EN_ESPERA
            )) {
                is AuthorizedActionHandler.CheckResult.HasAccess -> {
                    // User has access, execute action directly
                    executePauseTransaction()
                }
                is AuthorizedActionHandler.CheckResult.RequiresAuthorization -> {
                    // Set callback and show authorization dialog
                    pendingAuthorizedAction = { _ -> executePauseTransaction() }
                    currentProcessCode = UserPermissions.PROCESS_TRANSACCION_EN_ESPERA
                    _state.update {
                        it.copy(
                            authorizationDialogState = AuthorizationDialogState(
                                isVisible = true,
                                title = "Pausar Transacción",
                                message = "Para pausar la transacción debe solicitar autorización",
                                actionButtonText = "Pausar Transacción",
                                processCode = UserPermissions.PROCESS_TRANSACCION_EN_ESPERA
                            )
                        )
                    }
                }
            }
        }
    }

    private fun submitAuthorization(userCode: String, password: String) {
        val dialogState = _state.value.authorizationDialogState
        val actionCallback = pendingAuthorizedAction

        if (actionCallback == null) {
            Log.e(TAG, "No pending authorization action")
            return
        }

        viewModelScope.launch {
            // Show loading
            _state.update {
                it.copy(
                    authorizationDialogState = dialogState.copy(isLoading = true, error = null)
                )
            }

            when (val result = authorizedActionHandler.submitAuthorization(userCode, password, currentProcessCode)) {
                is AuthorizedActionHandler.AuthorizationResult.Success -> {
                    // Authorization successful - close dialog and execute pending action
                    _state.update {
                        it.copy(authorizationDialogState = AuthorizationDialogState())
                    }

                    // Execute the pending callback with the authorizing user
                    actionCallback(result.authorizedBy)
                    pendingAuthorizedAction = null
                    currentProcessCode = ""
                }
                is AuthorizedActionHandler.AuthorizationResult.Failed -> {
                    Log.e(TAG, "Authorization failed: ${result.message}")
                    _state.update {
                        it.copy(
                            authorizationDialogState = dialogState.copy(
                                isLoading = false,
                                error = result.message
                            )
                        )
                    }
                }
            }
        }
    }

    private fun executeDeleteLine(itemId: String, authorizedBy: String? = null) {
        Log.d(TAG, "Executing delete line for item: $itemId, authorizedBy: $authorizedBy")
        viewModelScope.launch {
            val authOperator = authorizedBy ?: sessionManager.getUserCode().first() ?: ""
            val affiliateType = _state.value.selectedCustomer?.affiliateType ?: "0001"
            val transactionId = _state.value.transactionCode

            _state.update { it.copy(isDeletingLine = true, deleteLineError = null) }

            voidItemUseCase(
                transactionId = transactionId,
                itemPosId = itemId,
                authorizedOperator = authOperator,
                affiliateType = affiliateType
            ).fold(
                onSuccess = {
                    Log.d(TAG, "Delete line success!")
                    _state.update { it.copy(isDeletingLine = false) }
                    loadTransactionDetails(transactionId)
                },
                onFailure = { error ->
                    Log.e(TAG, "Delete line error: ${error.message}")
                    _state.update {
                        it.copy(
                            isDeletingLine = false,
                            deleteLineError = error.message
                        )
                    }
                }
            )
        }
    }

    private fun executeChangeQuantity(itemId: String, authorizedBy: String?) {
        Log.d(TAG, "Executing change quantity for item: $itemId, authorizedBy: $authorizedBy")
        // This is called after authorization - show the quantity dialog with the authorizer's code
        // Item info should already be in state from handleChangeQuantityRequest
        val state = _state.value
        if (state.changeQuantityItemId == itemId) {
            _state.update {
                it.copy(
                    showChangeQuantityDialog = true,
                    changeQuantityNewQty = state.changeQuantityCurrentQty.toInt().toString(),
                    changeQuantityAuthorizedBy = authorizedBy
                )
            }
        } else {
            // Find the item if not already in state
            val item = _state.value.invoiceData.items.find { it.itemId == itemId }
            if (item != null) {
                showChangeQuantityDialog(itemId, item.itemName, item.lineItemSequence, item.quantity, authorizedBy)
            } else {
                Log.e(TAG, "Item not found for change quantity: $itemId")
            }
        }
    }

    private fun confirmChangeQuantity() {
        val state = _state.value
        val newQty = state.changeQuantityNewQty.toDoubleOrNull()

        if (newQty == null || newQty < 1 || newQty > 99) {
            _state.update { it.copy(changeQuantityError = "La cantidad debe ser entre 1 y 99") }
            return
        }

        // Use the stored authorizedBy - no need to check permission again
        // Authorization was already done before showing this dialog
        executeChangeQuantityApi(
            itemId = state.changeQuantityItemId,
            lineNumber = state.changeQuantityLineNumber,
            newQuantity = newQty,
            authorizedBy = state.changeQuantityAuthorizedBy
        )
    }

    private fun executeChangeQuantityApi(
        itemId: String,
        lineNumber: Int,
        newQuantity: Double,
        authorizedBy: String?
    ) {
        Log.d(TAG, "Executing change quantity API - itemId: $itemId, lineNumber: $lineNumber, newQuantity: $newQuantity, authorizedBy: $authorizedBy")
        viewModelScope.launch {
            val affiliateType = _state.value.selectedCustomer?.affiliateType ?: "0001"
            val transactionId = _state.value.transactionCode

            _state.update { it.copy(isChangingQuantity = true, changeQuantityError = null) }

            changeQuantityUseCase(
                transactionId = transactionId,
                itemPosId = itemId,
                lineNumber = lineNumber,
                newQuantity = newQuantity,
                affiliateType = affiliateType,
                authorizedBy = authorizedBy
            ).fold(
                onSuccess = { invoiceData ->
                    Log.d(TAG, "Change quantity success!")
                    _state.update {
                        it.copy(
                            isChangingQuantity = false,
                            showChangeQuantityDialog = false,
                            changeQuantityNewQty = "",
                            changeQuantityItemId = "",
                            changeQuantityItemName = "",
                            changeQuantityLineNumber = 0,
                            changeQuantityCurrentQty = 0.0,
                            changeQuantityAuthorizedBy = null,
                            invoiceData = invoiceData
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Change quantity error: ${error.message}")
                    _state.update {
                        it.copy(
                            isChangingQuantity = false,
                            changeQuantityError = error.message
                        )
                    }
                }
            )
        }
    }

    private fun executeAbortTransaction(authorizedBy: String? = null) {
        Log.d(TAG, "Executing abort transaction - showing confirmation dialog, authorizedBy: $authorizedBy")
        viewModelScope.launch {
            val authOperator = authorizedBy ?: sessionManager.getUserCode().first() ?: ""
            _state.update {
                it.copy(
                    transactionControl = it.transactionControl.copy(
                        showAbortConfirmDialog = true,
                        abortReason = "",
                        abortAuthorizingOperator = authOperator
                    )
                )
            }
        }
    }

    private fun confirmAbortTransaction() {
        Log.d(TAG, "confirmAbortTransaction() called")
        val transactionCode = _state.value.transactionCode
        val reason = _state.value.transactionControl.abortReason.trim()
        val authorizingOperator = _state.value.transactionControl.abortAuthorizingOperator

        if (reason.isBlank()) {
            Log.e(TAG, "No abort reason provided")
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    transactionControl = it.transactionControl.copy(
                        isAbortingTransaction = true,
                        showAbortConfirmDialog = false,
                        abortTransactionError = null
                    )
                )
            }

            abortTransactionUseCase(
                transactionId = transactionCode,
                reason = reason,
                authorizingOperator = authorizingOperator
            ).fold(
                onSuccess = {
                    Log.d(TAG, "Abort success!")
                    _state.update {
                        it.copy(
                            transactionControl = TransactionControlState(shouldNavigateAfterAbort = true),
                            transactionCode = "",
                            isTransactionCreated = false,
                            invoiceData = InvoiceData(),
                            selectedCustomer = null
                        )
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Abort error: ${error.message}")
                    _state.update {
                        it.copy(
                            transactionControl = it.transactionControl.copy(
                                isAbortingTransaction = false,
                                abortTransactionError = error.message
                            )
                        )
                    }
                }
            )
        }
    }

    private fun executeAddMaterialWithAuthorization(
        itemPosId: String,
        quantity: Double,
        partyAffiliationTypeCode: String?,
        authorizedBy: String?
    ) {
        Log.d(TAG, "Executing add material with authorization for item: $itemPosId, authorizedBy: $authorizedBy")
        viewModelScope.launch {
            val currentTransactionCode = _state.value.transactionCode

            var sessionId: String? = null
            var workstationId: String? = null

            if (currentTransactionCode.isBlank()) {
                sessionId = sessionManager.getSessionId().first()
                workstationId = sessionManager.getStationId().first()

                if (sessionId.isNullOrBlank() || workstationId.isNullOrBlank()) {
                    _state.update {
                        it.copy(addArticleError = "No hay sesión activa")
                    }
                    return@launch
                }
            }

            val selectedCustomer = _state.value.selectedCustomer
            val customerId = if (currentTransactionCode.isBlank()) selectedCustomer?.partyId?.toString() else null
            val customerIdType = if (currentTransactionCode.isBlank()) selectedCustomer?.identificationType else null
            val customerName = if (currentTransactionCode.isBlank()) selectedCustomer?.name else null

            billingRepository.addMaterial(
                transactionId = currentTransactionCode,
                itemPosId = itemPosId,
                quantity = quantity,
                partyAffiliationTypeCode = partyAffiliationTypeCode,
                sessionId = sessionId,
                workstationId = workstationId,
                customerId = customerId,
                customerIdType = customerIdType,
                customerName = customerName,
                isAuthorized = true,
                authorizedBy = authorizedBy
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update {
                            it.copy(
                                isAddingArticle = true,
                                addArticleError = null
                            )
                        }
                    }
                    is Resource.Success -> {
                        val addResult = result.data!!
                        // Save transactionId to local database if new transaction was created
                        if (addResult.transactionId != null && _state.value.transactionCode.isBlank()) {
                            billingRepository.saveActiveTransactionId(addResult.transactionId)
                        }
                        _state.update {
                            it.copy(
                                isAddingArticle = false,
                                transactionCode = addResult.transactionId ?: it.transactionCode,
                                isTransactionCreated = addResult.transactionId != null || it.isTransactionCreated,
                                invoiceData = addResult.invoiceData
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isAddingArticle = false,
                                addArticleError = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun executePauseTransaction() {
        Log.d(TAG, "Executing pause transaction - showing confirmation dialog")
        _state.update {
            it.copy(transactionControl = it.transactionControl.copy(showPauseConfirmDialog = true))
        }
    }

    private fun confirmPauseTransaction() {
        Log.d(TAG, "confirmPauseTransaction() called")
        val transactionCode = _state.value.transactionCode
        val currentState = _state.value
        val totalItems = currentState.invoiceData.items.getTotalItemCount()
        val subtotal = currentState.invoiceData.totals.subTotal

        viewModelScope.launch {
            _state.update {
                it.copy(
                    transactionControl = it.transactionControl.copy(
                        isPausingTransaction = true,
                        showPauseConfirmDialog = false,
                        pauseTransactionError = null
                    )
                )
            }

            when (val result = pauseTransactionUseCase(
                transactionId = transactionCode,
                totalItems = totalItems,
                subtotal = subtotal
            )) {
                is PauseTransactionUseCase.PauseResult.Success -> {
                    Log.d(TAG, "Pause and print success!")
                    _state.update {
                        it.copy(
                            transactionControl = it.transactionControl.copy(
                                isPausingTransaction = false,
                                shouldNavigateAfterPause = true
                            ),
                            printState = PrintState(),
                            transactionCode = "",
                            isTransactionCreated = false,
                            invoiceData = InvoiceData(),
                            selectedCustomer = null
                        )
                    }
                }
                is PauseTransactionUseCase.PauseResult.PrintFailed -> {
                    Log.e(TAG, "Pause success but print failed: ${result.printError}")
                    _state.update {
                        it.copy(
                            transactionControl = it.transactionControl.copy(isPausingTransaction = false),
                            printState = it.printState.copy(
                                isPrinting = false,
                                showPrintErrorDialog = true,
                                printErrorMessage = result.printError,
                                pendingPrintText = result.printText
                            )
                        )
                    }
                }
                is PauseTransactionUseCase.PauseResult.Failed -> {
                    Log.e(TAG, "Pause failed: ${result.message}")
                    _state.update {
                        it.copy(
                            transactionControl = it.transactionControl.copy(
                                isPausingTransaction = false,
                                pauseTransactionError = result.message
                            )
                        )
                    }
                }
            }
        }
    }

    private fun retryPrint() {
        val printText = _state.value.printState.pendingPrintText
        val printTransactionCode = _state.value.printState.pendingPrintTransactionCode
        // Use isReprint based on whether documents were retrieved before the previous failure
        val isReprint = _state.value.printState.documentsRetrievedBeforeFail
        Log.d(TAG, "Retry print with isReprint=$isReprint")

        viewModelScope.launch {
            _state.update {
                it.copy(
                    printState = it.printState.copy(
                        showPrintErrorDialog = false,
                        printErrorMessage = null,
                        isPrinting = true
                    )
                )
            }

            when {
                // Case 1: Retry printing pause receipt (direct text)
                !printText.isNullOrBlank() -> {
                    printerManager.printText(printText)
                        .onSuccess {
                            Log.d(TAG, "Retry print pause receipt successful")
                            _state.update {
                                it.copy(
                                    printState = PrintState(),
                                    transactionControl = it.transactionControl.copy(shouldNavigateAfterPause = true),
                                    transactionCode = "",
                                    isTransactionCreated = false,
                                    invoiceData = InvoiceData(),
                                    selectedCustomer = null
                                )
                            }
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Retry print pause receipt failed: ${error.message}")
                            _state.update {
                                it.copy(
                                    printState = it.printState.copy(
                                        isPrinting = false,
                                        showPrintErrorDialog = true,
                                        printErrorMessage = error.message ?: "Error al imprimir"
                                    )
                                )
                            }
                        }
                }

                // Case 2: Retry printing finalize documents
                !printTransactionCode.isNullOrBlank() -> {
                    printDocumentsUseCase(printTransactionCode, isReprint = isReprint)
                        .onSuccess { printedCount ->
                            Log.d(TAG, "Retry print documents successful: $printedCount documents")
                            _state.update {
                                it.copy(
                                    printState = PrintState(),
                                    shouldNavigateBackToBilling = true
                                )
                            }
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Retry print documents failed: ${error.message}")
                            val docsRetrieved = error is PrinterFailureException
                            _state.update {
                                it.copy(
                                    printState = it.printState.copy(
                                        isPrinting = false,
                                        showPrintErrorDialog = true,
                                        printErrorMessage = error.message ?: "Error al imprimir los documentos",
                                        documentsRetrievedBeforeFail = docsRetrieved
                                    )
                                )
                            }
                        }
                }

                else -> {
                    Log.e(TAG, "No pending print to retry")
                    skipPrint()
                }
            }
        }
    }

    private fun skipPrint() {
        val isPauseReceipt = _state.value.printState.pendingPrintText != null
        val isFinalizeDocuments = _state.value.printState.pendingPrintTransactionCode != null

        Log.d(TAG, "Skipping print - isPauseReceipt: $isPauseReceipt, isFinalizeDocuments: $isFinalizeDocuments")

        if (isFinalizeDocuments) {
            // Skip printing finalize documents - just navigate to new transaction
            _state.update {
                it.copy(
                    printState = PrintState(),
                    shouldNavigateBackToBilling = true
                )
            }
        } else {
            // Skip printing pause receipt - reset state and navigate
            _state.update {
                it.copy(
                    printState = PrintState(),
                    transactionControl = it.transactionControl.copy(shouldNavigateAfterPause = true),
                    transactionCode = "",
                    isTransactionCreated = false,
                    invoiceData = InvoiceData(),
                    selectedCustomer = null
                )
            }
        }
    }

    // Packaging methods

    private fun openPackagingDialog() {
        val transactionId = _state.value.transactionCode
        if (transactionId.isBlank()) {
            Log.e(TAG, "No transaction code for packaging")
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    packagingState = it.packagingState.copy(
                        isVisible = true,
                        isLoading = true,
                        loadError = null
                    )
                )
            }

            getPackagingReconciliationUseCase(transactionId).fold(
                onSuccess = { packagingItems ->
                    val initialInputs = packagingItems.associate { it.itemPosId to "" }
                    _state.update {
                        it.copy(
                            packagingState = it.packagingState.copy(
                                isLoading = false,
                                items = packagingItems,
                                inputs = initialInputs
                            )
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            packagingState = it.packagingState.copy(
                                isLoading = false,
                                loadError = error.message
                            )
                        )
                    }
                }
            )
        }
    }

    private fun submitPackagings() {
        val transactionId = _state.value.transactionCode
        val packagingItems = _state.value.packagingState.items
        val packagingInputs = _state.value.packagingState.inputs
        val affiliateType = _state.value.selectedCustomer?.affiliateType ?: "0001"

        viewModelScope.launch {
            _state.update {
                it.copy(
                    packagingState = it.packagingState.copy(
                        isUpdating = true,
                        updateError = null
                    )
                )
            }

            updatePackagingsUseCase(
                transactionId = transactionId,
                packagingItems = packagingItems,
                packagingInputs = packagingInputs,
                affiliateType = affiliateType
            ).fold(
                onSuccess = {
                    Log.d(TAG, "Packagings updated successfully")
                    _state.update {
                        it.copy(packagingState = PackagingDialogState())
                    }
                    loadTransactionDetails(transactionId)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            packagingState = it.packagingState.copy(
                                isUpdating = false,
                                updateError = error.message
                            )
                        )
                    }
                }
            )
        }
    }
}
