package com.devlosoft.megaposmobile.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.BuildConfig
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.dataphone.DataphoneManager
import com.devlosoft.megaposmobile.core.printer.LocalPrintTemplates
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.core.state.DataphoneState
import com.devlosoft.megaposmobile.core.state.StationState
import com.devlosoft.megaposmobile.core.state.StationStatus
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.data.remote.dto.PaxCloseResponseDto
import com.devlosoft.megaposmobile.domain.repository.PaymentRepository
import com.devlosoft.megaposmobile.domain.usecase.AuthorizeProcessUseCase
import com.devlosoft.megaposmobile.domain.usecase.CheckVersionUseCase
import com.devlosoft.megaposmobile.domain.usecase.CloseTerminalUseCase
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.devlosoft.megaposmobile.domain.usecase.LogoutUseCase
import com.devlosoft.megaposmobile.domain.usecase.OpenTerminalUseCase
import com.devlosoft.megaposmobile.presentation.shared.components.AuthorizationDialogState
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val openTerminalUseCase: OpenTerminalUseCase,
    private val closeTerminalUseCase: CloseTerminalUseCase,
    private val sessionManager: SessionManager,
    private val serverConfigDao: ServerConfigDao,
    private val stationStatus: StationStatus,
    private val printerManager: PrinterManager,
    private val authorizeProcessUseCase: AuthorizeProcessUseCase,
    private val dataphoneManager: DataphoneManager,
    private val paymentRepository: PaymentRepository,
    private val gson: Gson,
    private val dataphoneState: DataphoneState,
    private val checkVersionUseCase: CheckVersionUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        // Load permissions synchronously BEFORE any UI rendering
        // This prevents the menu from appearing "laggy" when loading elements
        val permissions = runBlocking { sessionManager.getUserPermissionsSync() }
        _state.value = _state.value.copy(
            userPermissions = permissions,
            canOpenTerminal = permissions?.shouldShow(UserPermissions.PROCESS_APERTURA_CAJA) ?: false,
            canCloseTerminal = permissions?.shouldShow(UserPermissions.PROCESS_CIERRE_CAJA) ?: false,
            canCloseDatafono = permissions?.shouldShow(UserPermissions.PROCESS_CIERRE_DATAFONO) ?: false,
            canBilling = permissions?.shouldShow(UserPermissions.PROCESS_FACTURAR) ?: false,
            canViewTransactions = permissions?.shouldShow(UserPermissions.PROCESS_REIMPRESION) ?: false,
            canAdvancedOptions = permissions?.shouldShow(UserPermissions.PROCESS_OPCIONES_AVANZADAS) ?: false
        )
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            // Get user name from session
            val userName = sessionManager.getUserName().first() ?: "Usuario"

            // Get business unit name (sucursal) from session
            val businessUnitName = sessionManager.getBusinessUnitName().first() ?: ""

            // Get terminal name (hostname) from server config
            val serverConfig = serverConfigDao.getActiveServerConfigSync()
            val terminalName = serverConfig?.serverName ?: "Terminal"

            // Get current date formatted
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("es", "ES"))
            val currentDate = dateFormat.format(Date())

            // Get station status (permissions are already loaded in init)
            stationStatus.state.collect { status ->
                _state.update {
                    it.copy(
                        userName = userName,
                        currentDate = currentDate,
                        terminalName = terminalName,
                        businessUnitName = businessUnitName,
                        stationStatus = stationStatus.getDisplayText(),
                        isStationOpen = status == StationState.OPEN,
                        isLoading = false
                    )
                }
            }
        }
    }

    private var onLogoutCallback: (() -> Unit)? = null
    private var onNavigateToBillingCallback: (() -> Unit)? = null
    private var onNavigateToProcessCallback: ((String) -> Unit)? = null
    private var onNavigateToAdvancedOptionsCallback: (() -> Unit)? = null
    private var onNavigateToTodayTransactionsCallback: (() -> Unit)? = null

    fun setLogoutCallback(callback: () -> Unit) {
        onLogoutCallback = callback
    }

    fun setNavigateToBillingCallback(callback: () -> Unit) {
        onNavigateToBillingCallback = callback
    }

    fun setNavigateToProcessCallback(callback: (String) -> Unit) {
        onNavigateToProcessCallback = callback
    }

    fun setNavigateToAdvancedOptionsCallback(callback: () -> Unit) {
        onNavigateToAdvancedOptionsCallback = callback
    }

    fun setNavigateToTodayTransactionsCallback(callback: () -> Unit) {
        onNavigateToTodayTransactionsCallback = callback
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OpenTerminal -> {
                openTerminal()
            }
            is HomeEvent.CloseTerminal -> {
                closeTerminal()
            }
            is HomeEvent.Billing -> {
                showTodoDialog("Facturación")
            }
            is HomeEvent.CheckPrinterAndNavigateToBilling -> {
                checkPrinterConnection()
            }
            is HomeEvent.DismissPrinterError -> {
                _state.update { it.copy(printerError = null) }
            }
            is HomeEvent.DailyTransactions -> {
                showTodoDialog("Transacciones del día")
            }
            is HomeEvent.DismissDialog -> {
                _state.update { it.copy(showTodoDialog = false, todoDialogTitle = "") }
            }
            is HomeEvent.ToggleUserMenu -> {
                _state.update { it.copy(showUserMenu = !it.showUserMenu) }
            }
            is HomeEvent.DismissUserMenu -> {
                _state.update { it.copy(showUserMenu = false) }
            }
            is HomeEvent.ShowLogoutConfirmDialog -> {
                _state.update { it.copy(showUserMenu = false, showLogoutConfirmDialog = true) }
            }
            is HomeEvent.DismissLogoutConfirmDialog -> {
                _state.update { it.copy(showLogoutConfirmDialog = false) }
            }
            is HomeEvent.ConfirmLogout -> {
                _state.update { it.copy(showLogoutConfirmDialog = false) }
                logout()
            }
            is HomeEvent.DismissOpenTerminalError -> {
                _state.update { it.copy(openTerminalError = null) }
            }
            is HomeEvent.DismissOpenTerminalSuccess -> {
                _state.update { it.copy(showOpenTerminalSuccessDialog = false, openTerminalMessage = "") }
            }
            is HomeEvent.DismissCloseTerminalError -> {
                _state.update { it.copy(closeTerminalError = null) }
            }
            is HomeEvent.DismissCloseTerminalSuccess -> {
                _state.update { it.copy(showCloseTerminalSuccessDialog = false, closeTerminalMessage = "") }
            }
            // Authorization events
            is HomeEvent.RequestOpenTerminal -> {
                handleRequestOpenTerminal()
            }
            is HomeEvent.RequestCloseTerminal -> {
                handleRequestCloseTerminal()
            }
            is HomeEvent.RequestCloseDatafono -> {
                handleRequestCloseDatafono()
            }
            is HomeEvent.RequestBilling -> {
                handleRequestBilling()
            }
            is HomeEvent.RequestViewTransactions -> {
                handleRequestViewTransactions()
            }
            is HomeEvent.RequestAdvancedOptions -> {
                handleRequestAdvancedOptions()
            }
            is HomeEvent.SubmitAuthorization -> {
                submitAuthorization(event.userCode, event.password)
            }
            is HomeEvent.DismissAuthorizationDialog -> {
                _state.update {
                    it.copy(
                        authorizationDialogState = AuthorizationDialogState(),
                        pendingAuthorizationAction = null
                    )
                }
            }
            is HomeEvent.ClearAuthorizationError -> {
                _state.update {
                    it.copy(
                        authorizationDialogState = it.authorizationDialogState.copy(error = null)
                    )
                }
            }
            // Close Datafono events
            is HomeEvent.ShowCloseDatafonoConfirmDialog -> {
                _state.update { it.copy(showCloseDatafonoConfirmDialog = true) }
            }
            is HomeEvent.DismissCloseDatafonoConfirmDialog -> {
                _state.update { it.copy(showCloseDatafonoConfirmDialog = false) }
            }
            is HomeEvent.ConfirmCloseDatafono -> {
                _state.update { it.copy(showCloseDatafonoConfirmDialog = false) }
                onNavigateToProcessCallback?.invoke("closeDataphone")
            }
            is HomeEvent.DismissCloseDatafonoError -> {
                _state.update { it.copy(closeDatafonoError = null) }
            }
            is HomeEvent.DismissCloseDatafonoSuccess -> {
                _state.update {
                    it.copy(
                        showCloseDatafonoSuccessDialog = false,
                        closeDatafonoMessage = "",
                        closeDatafonoSalesCount = 0,
                        closeDatafonoSalesTotal = 0.0
                    )
                }
            }
        }
    }

    private fun openTerminal() {
        viewModelScope.launch {
            openTerminalUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isOpeningTerminal = true, openTerminalError = null) }
                    }
                    is Resource.Success -> {
                        result.data?.let { openStationResult ->
                            // Save sessionId and stationId to SessionManager
                            sessionManager.saveStationInfo(
                                sessionId = openStationResult.sessionId,
                                stationId = openStationResult.stationId
                            )

                            // Update global station status
                            stationStatus.open()

                            val message = if (openStationResult.isNewSession) {
                                "Terminal aperturada exitosamente"
                            } else {
                                "Terminal ya estaba aperturada"
                            }

                            _state.update {
                                it.copy(
                                    isOpeningTerminal = false,
                                    showOpenTerminalSuccessDialog = true,
                                    openTerminalMessage = message,
                                    stationStatus = stationStatus.getDisplayText()
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isOpeningTerminal = false,
                                openTerminalError = result.message ?: "Error al aperturar terminal"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun closeTerminal() {
        viewModelScope.launch {
            closeTerminalUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isClosingTerminal = true, closeTerminalError = null) }
                    }
                    is Resource.Success -> {
                        result.data?.let { closeStationResult ->
                            // Update global station status
                            stationStatus.close()

                            val message = if (closeStationResult.success) {
                                "Terminal cerrada exitosamente"
                            } else {
                                "No se pudo cerrar la terminal"
                            }

                            _state.update {
                                it.copy(
                                    isClosingTerminal = false,
                                    showCloseTerminalSuccessDialog = closeStationResult.success,
                                    closeTerminalMessage = message,
                                    closeTerminalError = if (!closeStationResult.success) message else null,
                                    stationStatus = stationStatus.getDisplayText()
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isClosingTerminal = false,
                                closeTerminalError = result.message ?: "Error al cerrar terminal"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showTodoDialog(title: String) {
        _state.update { it.copy(showTodoDialog = true, todoDialogTitle = title) }
    }

    private fun logout() {
        viewModelScope.launch {
            // Close station status when logging out
            stationStatus.close()

            logoutUseCase().collect { }
            onLogoutCallback?.invoke()
        }
    }

    private fun checkPrinterConnection() {
        viewModelScope.launch {
            // Skip printer test in development mode
            if (BuildConfig.DEVELOPMENT_MODE) {
                onNavigateToBillingCallback?.invoke()
                return@launch
            }

            _state.update { it.copy(isCheckingPrinter = true, printerError = null) }

            try {
                // Test printer connection using PrinterManager
                val result = printerManager.testPrinterConnection()

                _state.update { it.copy(isCheckingPrinter = false) }

                result.fold(
                    onSuccess = {
                        // Printer is connected, navigate to billing
                        onNavigateToBillingCallback?.invoke()
                    },
                    onFailure = { exception ->
                        // Printer is not connected, show error
                        _state.update {
                            it.copy(printerError = exception.message ?: "Error al conectar con la impresora")
                        }
                    }
                )

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isCheckingPrinter = false,
                        printerError = "Error inesperado: ${e.message}"
                    )
                }
            }
        }
    }

    // Authorization handlers

    private fun handleRequestOpenTerminal() {
        val hasAccess = _state.value.userPermissions?.hasAccess(UserPermissions.PROCESS_APERTURA_CAJA) ?: false
        if (hasAccess) {
            onNavigateToProcessCallback?.invoke("openTerminal")
        } else {
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Aperturar Terminal",
                        message = "Para aperturar la terminal debe solicitar autorización",
                        actionButtonText = "Aperturar Terminal",
                        processCode = UserPermissions.PROCESS_APERTURA_CAJA
                    ),
                    pendingAuthorizationAction = HomePendingAction.OpenTerminal
                )
            }
        }
    }

    private fun handleRequestCloseTerminal() {
        val hasAccess = _state.value.userPermissions?.hasAccess(UserPermissions.PROCESS_CIERRE_CAJA) ?: false
        if (hasAccess) {
            onNavigateToProcessCallback?.invoke("closeTerminal")
        } else {
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Cierre Terminal",
                        message = "Para cerrar la terminal debe solicitar autorización",
                        actionButtonText = "Cierre Terminal",
                        processCode = UserPermissions.PROCESS_CIERRE_CAJA
                    ),
                    pendingAuthorizationAction = HomePendingAction.CloseTerminal
                )
            }
        }
    }

    private fun handleRequestCloseDatafono() {
        val hasAccess = _state.value.userPermissions?.hasAccess(UserPermissions.PROCESS_CIERRE_DATAFONO) ?: false
        if (hasAccess) {
            // Show confirmation dialog
            _state.update { it.copy(showCloseDatafonoConfirmDialog = true) }
        } else {
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Cierre de Datafono",
                        message = "Para cerrar el datafono debe solicitar autorización",
                        actionButtonText = "Cierre Datafono",
                        processCode = UserPermissions.PROCESS_CIERRE_DATAFONO
                    ),
                    pendingAuthorizationAction = HomePendingAction.CloseDatafono
                )
            }
        }
    }

    private fun handleRequestBilling() {
        viewModelScope.launch {
            // First verify app version
            checkVersionUseCase().collect { versionResult ->
                when (versionResult) {
                    is Resource.Loading -> {
                        _state.update { it.copy(isCheckingPrinter = true) }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isCheckingPrinter = false,
                                printerError = versionResult.message ?: "Error al verificar versión"
                            )
                        }
                        return@collect
                    }
                    is Resource.Success -> {
                        if (versionResult.data?.isValid != true) {
                            _state.update {
                                it.copy(
                                    isCheckingPrinter = false,
                                    printerError = versionResult.data?.errorMessage ?: "Versión no válida"
                                )
                            }
                            return@collect
                        }

                        // Version valid - continue with billing flow
                        _state.update { it.copy(isCheckingPrinter = false) }
                        checkBillingPermissionAndProceed()
                    }
                }
            }
        }
    }

    private fun checkBillingPermissionAndProceed() {
        val hasAccess = _state.value.userPermissions?.hasAccess(UserPermissions.PROCESS_FACTURAR) ?: false
        if (hasAccess) {
            checkPrinterConnection()
        } else {
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Facturación",
                        message = "Para facturar debe solicitar autorización",
                        actionButtonText = "Facturar",
                        processCode = UserPermissions.PROCESS_FACTURAR
                    ),
                    pendingAuthorizationAction = HomePendingAction.Billing
                )
            }
        }
    }

    private fun handleRequestViewTransactions() {
        val hasAccess = _state.value.userPermissions?.hasAccess(UserPermissions.PROCESS_REIMPRESION) ?: false
        if (hasAccess) {
            onNavigateToTodayTransactionsCallback?.invoke()
        } else {
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Transacciones del Día",
                        message = "Para ver las transacciones debe solicitar autorización",
                        actionButtonText = "Ver Transacciones",
                        processCode = UserPermissions.PROCESS_REIMPRESION
                    ),
                    pendingAuthorizationAction = HomePendingAction.ViewTransactions
                )
            }
        }
    }

    private fun handleRequestAdvancedOptions() {
        val hasAccess = _state.value.userPermissions?.hasAccess(UserPermissions.PROCESS_OPCIONES_AVANZADAS) ?: false
        if (hasAccess) {
            onNavigateToAdvancedOptionsCallback?.invoke()
        } else {
            _state.update {
                it.copy(
                    authorizationDialogState = AuthorizationDialogState(
                        isVisible = true,
                        title = "Opciones Avanzadas",
                        message = "Para acceder a opciones avanzadas debe solicitar autorización",
                        actionButtonText = "Acceder",
                        processCode = UserPermissions.PROCESS_OPCIONES_AVANZADAS
                    ),
                    pendingAuthorizationAction = HomePendingAction.AdvancedOptions
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

    private fun executePendingAction(pendingAction: HomePendingAction) {
        when (pendingAction) {
            is HomePendingAction.OpenTerminal -> onNavigateToProcessCallback?.invoke("openTerminal")
            is HomePendingAction.CloseTerminal -> onNavigateToProcessCallback?.invoke("closeTerminal")
            is HomePendingAction.CloseDatafono -> onNavigateToProcessCallback?.invoke("closeDataphone")
            is HomePendingAction.Billing -> checkPrinterConnection() // Version was already checked in handleRequestBilling
            is HomePendingAction.ViewTransactions -> onNavigateToTodayTransactionsCallback?.invoke()
            is HomePendingAction.AdvancedOptions -> onNavigateToAdvancedOptionsCallback?.invoke()
        }
    }

    private fun closeDatafono() {
        viewModelScope.launch {
            _state.update { it.copy(isClosingDatafono = true, closeDatafonoError = null) }

            try {
                // Step 1: Get point of sale code from session
                val pointOfSaleCode = sessionManager.getStationId().first() ?: run {
                    _state.update {
                        it.copy(
                            isClosingDatafono = false,
                            closeDatafonoError = "No se encontró el código de punto de venta"
                        )
                    }
                    return@launch
                }

                // Step 2: Call PAX to execute the close (or simulation)
                val dataphoneResultWrapper = dataphoneManager.closeDataphone()

                val dataphoneResult = dataphoneResultWrapper.getOrElse { error ->
                    _state.update {
                        it.copy(
                            isClosingDatafono = false,
                            closeDatafonoError = error.message ?: "Error al cerrar el datáfono"
                        )
                    }
                    return@launch
                }

                if (!dataphoneResult.success) {
                    _state.update {
                        it.copy(
                            isClosingDatafono = false,
                            closeDatafonoError = dataphoneResult.errorMessage ?: "Error al cerrar el datáfono"
                        )
                    }
                    return@launch
                }

                // Step 3: Build the PaxCloseResponseDto from the result
                val paxResponse = PaxCloseResponseDto(
                    baseAmount = "CRC0.00",
                    cardholder = "",
                    recibo = "000000",
                    stan = "000000",
                    taxAmount = "CRC0.00",
                    ticket = dataphoneResult.ticket ?: "",
                    tipAmount = "CRC0.00",
                    totalAmount = "CRC0.00",
                    txnId = "EVENTOS"
                )

                // Step 4: Send the close data to megapos backend
                // Use the terminal ID from global state, fallback to PAX result if not available
                val terminalId = dataphoneState.getTerminalId().ifBlank {
                    dataphoneResult.terminal ?: ""
                }
                Log.d(TAG, "Using terminal ID for close: $terminalId")
                paymentRepository.closeDataphone(pointOfSaleCode, terminalId, paxResponse).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            // Already showing loading
                        }
                        is Resource.Success -> {
                            val response = result.data

                            // Imprimir comprobante de cierre
                            try {
                                val userName = sessionManager.getUserName().first() ?: "Usuario"
                                val businessUnitName = sessionManager.getBusinessUnitName().first() ?: "Megasuper"
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm:ss a", Locale("es", "CR"))

                                val receiptText = LocalPrintTemplates.buildDataphoneCloseReceipt(
                                    userName = userName,
                                    terminalId = response?.terminalId ?: terminalId,
                                    closeDate = response?.closeDate ?: dateFormat.format(Date()),
                                    salesCount = response?.salesCount ?: dataphoneResult.salesCount,
                                    salesTotal = response?.salesTotal ?: dataphoneResult.salesTotal,
                                    reversalsCount = response?.reversalsCount ?: 0,
                                    reversalsTotal = response?.reversalsTotal ?: 0.0,
                                    netTotal = response?.netTotal ?: dataphoneResult.salesTotal,
                                    voucher = response?.voucher,
                                    businessUnitName = businessUnitName
                                )

                                printerManager.printText(receiptText)
                                Log.d(TAG, "Comprobante de cierre impreso exitosamente")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al imprimir comprobante de cierre", e)
                            }

                            _state.update {
                                it.copy(
                                    isClosingDatafono = false,
                                    showCloseDatafonoSuccessDialog = true,
                                    closeDatafonoMessage = response?.message ?: "Cierre completado exitosamente",
                                    closeDatafonoSalesCount = response?.salesCount ?: dataphoneResult.salesCount,
                                    closeDatafonoSalesTotal = response?.salesTotal ?: dataphoneResult.salesTotal
                                )
                            }
                        }
                        is Resource.Error -> {
                            _state.update {
                                it.copy(
                                    isClosingDatafono = false,
                                    closeDatafonoError = result.message ?: "Error al enviar cierre a megapos"
                                )
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during closeDatafono", e)
                _state.update {
                    it.copy(
                        isClosingDatafono = false,
                        closeDatafonoError = "Error inesperado: ${e.localizedMessage}"
                    )
                }
            }
        }
    }
}
