package com.devlosoft.megaposmobile.presentation.billing

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.model.Customer
import com.devlosoft.megaposmobile.domain.model.InvoiceData
import com.devlosoft.megaposmobile.domain.model.PrintDocument
import com.devlosoft.megaposmobile.domain.model.PrinterModel
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.devlosoft.megaposmobile.domain.repository.BillingRepository
import com.devlosoft.megaposmobile.domain.usecase.AuthorizeProcessUseCase
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
    private val serverConfigDao: ServerConfigDao,
    private val printerManager: PrinterManager,
    private val authorizeProcessUseCase: AuthorizeProcessUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "BillingViewModel"
    }

    private val _state = MutableStateFlow(BillingState())
    val state: StateFlow<BillingState> = _state.asStateFlow()

    init {
        loadUserPermissions()
        checkTransactionRecovery()
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
                _state.update {
                    it.copy(
                        authorizationDialogState = AuthorizationDialogState(),
                        pendingAuthorizationAction = null
                    )
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

            billingRepository.canRecoverTransaction(sessionId, stationId).collect { result ->
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

            billingRepository.addMaterial(
                transactionId = currentTransactionCode,
                itemPosId = articleId,
                quantity = 1.0,
                partyAffiliationTypeCode = selectedCustomer?.affiliateType,
                sessionId = sessionId,
                workstationId = workstationId,
                customerId = customerId,
                customerIdType = customerIdType,
                customerName = customerName
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

        billingRepository.getPrintDocuments(
            transactionId = transactionCode,
            templateId = "01-FC",
            isReprint = false,
            copyNumber = 0
        ).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    Log.d(TAG, "Fetching print documents...")
                }
                is Resource.Success -> {
                    val documents = result.data ?: emptyList()
                    Log.d(TAG, "Received ${documents.size} print documents")

                    // Print each document
                    for (document in documents) {
                        printDocument(document)
                    }

                    // Update state after printing
                    _state.update {
                        it.copy(
                            isFinalizingTransaction = false,
                            isTransactionFinalized = true,
                            shouldNavigateBackToBilling = true
                        )
                    }
                    Log.d(TAG, "State updated successfully after printing")
                }
                is Resource.Error -> {
                    Log.e(TAG, "Error fetching print documents: ${result.message}")
                    // Still finalize the transaction UI, just log the print error
                    _state.update {
                        it.copy(
                            isFinalizingTransaction = false,
                            isTransactionFinalized = true,
                            shouldNavigateBackToBilling = true
                        )
                    }
                }
            }
        }
    }

    private suspend fun printDocument(document: PrintDocument) {
        Log.d(TAG, "Printing document: ${document.documentType}")

        try {
            val config = serverConfigDao.getActiveServerConfigSync()

            if (config == null) {
                Log.e(TAG, "No server config found")
                return
            }

            val printText = document.printText
            Log.d(TAG, "Print text length: ${printText.length}")

            val printerModel = PrinterModel.fromString(config.printerModel)
            Log.d(TAG, "Using printer model: ${printerModel.displayName}")

            // Print using PrinterManager (handles both IP and Bluetooth internally)
            val result = printerManager.printText(printText)

            result.fold(
                onSuccess = { message ->
                    Log.d(TAG, "Print success: $message")
                },
                onFailure = { exception ->
                    Log.e(TAG, "Print error: ${exception.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception while printing: ${e.message}", e)
        }
    }

    // Authorization methods

    private fun handleDeleteLineRequest(itemId: String, itemName: String) {
        val permissions = _state.value.userPermissions
        val hasAccess = permissions?.hasAccess(UserPermissions.PROCESS_ELIMINAR_LINEA) ?: false

        if (hasAccess) {
            // User has access, execute action directly
            executeDeleteLine(itemId)
        } else {
            // User doesn't have access, show authorization dialog
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Eliminar Línea",
                        message = "Para eliminar el articulo $itemName debe solicitar autorización",
                        actionButtonText = "Eliminar Línea",
                        processCode = UserPermissions.PROCESS_ELIMINAR_LINEA
                    ),
                    pendingAuthorizationAction = PendingAuthorizationAction.DeleteLine(itemId)
                )
            }
        }
    }

    private fun handleChangeQuantityRequest(itemId: String, itemName: String) {
        val permissions = _state.value.userPermissions
        val hasAccess = permissions?.hasAccess(UserPermissions.PROCESS_CAMBIAR_CANTIDAD_ARTICULO) ?: false

        if (hasAccess) {
            // User has access, execute action directly
            executeChangeQuantity(itemId)
        } else {
            // User doesn't have access, show authorization dialog
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Cambiar Cantidad",
                        message = "Para cambiar la cantidad del articulo $itemName debe solicitar autorización",
                        actionButtonText = "Cambiar Cantidad",
                        processCode = UserPermissions.PROCESS_CAMBIAR_CANTIDAD_ARTICULO
                    ),
                    pendingAuthorizationAction = PendingAuthorizationAction.ChangeQuantity(itemId)
                )
            }
        }
    }

    private fun handleAbortTransactionRequest() {
        val permissions = _state.value.userPermissions
        val hasAccess = permissions?.hasAccess(UserPermissions.PROCESS_ABORTAR_TRANSACCION) ?: false

        if (hasAccess) {
            // User has access, execute action directly
            executeAbortTransaction()
        } else {
            // User doesn't have access, show authorization dialog
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Abortar Transacción",
                        message = "Para abortar la transacción debe solicitar autorización",
                        actionButtonText = "Abortar Transacción",
                        processCode = UserPermissions.PROCESS_ABORTAR_TRANSACCION
                    ),
                    pendingAuthorizationAction = PendingAuthorizationAction.AbortTransaction
                )
            }
        }
    }

    private fun handlePauseTransactionRequest() {
        val permissions = _state.value.userPermissions
        val hasAccess = permissions?.hasAccess(UserPermissions.PROCESS_TRANSACCION_EN_ESPERA) ?: false

        if (hasAccess) {
            // User has access, execute action directly
            executePauseTransaction()
        } else {
            // User doesn't have access, show authorization dialog
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Pausar Transacción",
                        message = "Para pausar la transacción debe solicitar autorización",
                        actionButtonText = "Pausar Transacción",
                        processCode = UserPermissions.PROCESS_TRANSACCION_EN_ESPERA
                    ),
                    pendingAuthorizationAction = PendingAuthorizationAction.PauseTransaction
                )
            }
        }
    }

    private fun submitAuthorization(userCode: String, password: String) {
        val dialogState = _state.value.authorizationDialogState
        val pendingAction = _state.value.pendingAuthorizationAction

        if (pendingAction == null) {
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

            authorizeProcessUseCase(userCode, password, dialogState.processCode)
                .onSuccess {
                    // Authorization successful - close dialog and execute pending action
                    _state.update {
                        it.copy(
                            authorizationDialogState = AuthorizationDialogState(),
                            pendingAuthorizationAction = null
                        )
                    }

                    // Execute the pending action
                    executePendingAction(pendingAction)
                }
                .onFailure { error ->
                    Log.e(TAG, "Authorization failed: ${error.message}")
                    _state.update {
                        it.copy(
                            authorizationDialogState = dialogState.copy(
                                isLoading = false,
                                error = error.message
                            )
                        )
                    }
                }
        }
    }

    private fun executePendingAction(pendingAction: PendingAuthorizationAction) {
        when (pendingAction) {
            is PendingAuthorizationAction.DeleteLine -> executeDeleteLine(pendingAction.itemId)
            is PendingAuthorizationAction.ChangeQuantity -> executeChangeQuantity(pendingAction.itemId)
            is PendingAuthorizationAction.AbortTransaction -> executeAbortTransaction()
            is PendingAuthorizationAction.PauseTransaction -> executePauseTransaction()
        }
    }

    // TODO: Implement these action methods when the actual functionality is added
    private fun executeDeleteLine(itemId: String) {
        Log.d(TAG, "Executing delete line for item: $itemId")
        _state.update {
            it.copy(
                showTodoDialog = true,
                todoDialogMessage = "Eliminar Línea\nItem ID: $itemId"
            )
        }
        // TODO: Implement delete line API call
    }

    private fun executeChangeQuantity(itemId: String) {
        Log.d(TAG, "Executing change quantity for item: $itemId")
        _state.update {
            it.copy(
                showTodoDialog = true,
                todoDialogMessage = "Cambiar Cantidad\nItem ID: $itemId"
            )
        }
        // TODO: Implement change quantity dialog/flow
    }

    private fun executeAbortTransaction() {
        Log.d(TAG, "Executing abort transaction")
        _state.update {
            it.copy(
                showTodoDialog = true,
                todoDialogMessage = "Abortar Transacción"
            )
        }
        // TODO: Implement abort transaction API call
    }

    private fun executePauseTransaction() {
        Log.d(TAG, "Executing pause transaction")
        _state.update {
            it.copy(
                showTodoDialog = true,
                todoDialogMessage = "Pausar Transacción"
            )
        }
        // TODO: Implement pause transaction API call
    }
}
