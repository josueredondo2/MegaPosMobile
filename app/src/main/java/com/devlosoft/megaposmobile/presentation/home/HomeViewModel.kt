package com.devlosoft.megaposmobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.BuildConfig
import com.devlosoft.megaposmobile.core.common.Resource
import com.devlosoft.megaposmobile.core.printer.PrinterManager
import com.devlosoft.megaposmobile.core.state.StationState
import com.devlosoft.megaposmobile.core.state.StationStatus
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.usecase.CloseTerminalUseCase
import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.devlosoft.megaposmobile.domain.usecase.LogoutUseCase
import com.devlosoft.megaposmobile.domain.usecase.OpenTerminalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val printerManager: PrinterManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            // Get user name from session
            val userName = sessionManager.getUserName().first() ?: "Usuario"

            // Get terminal name (hostname) from server config
            val serverConfig = serverConfigDao.getActiveServerConfigSync()
            val terminalName = serverConfig?.serverName ?: "Terminal"

            // Get current date formatted
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("es", "ES"))
            val currentDate = dateFormat.format(Date())

            // Get user permissions
            val permissions = sessionManager.getUserPermissionsSync()

            // Get station status
            stationStatus.state.collect { status ->
                _state.update {
                    it.copy(
                        userName = userName,
                        currentDate = currentDate,
                        terminalName = terminalName,
                        stationStatus = stationStatus.getDisplayText(),
                        isStationOpen = status == StationState.OPEN,
                        isLoading = false,
                        // Set permissions for menu items (using 'show' property)
                        canOpenTerminal = permissions?.shouldShow(UserPermissions.PROCESS_APERTURA_CAJA) ?: false,
                        canCloseTerminal = permissions?.shouldShow(UserPermissions.PROCESS_CIERRE_CAJA) ?: false,
                        canCloseDatafono = permissions?.shouldShow(UserPermissions.PROCESS_CIERRE_DATAFONO) ?: false,
                        canBilling = permissions?.shouldShow(UserPermissions.PROCESS_FACTURAR) ?: false,
                        canViewTransactions = permissions?.shouldShow(UserPermissions.PROCESS_REIMPRESION) ?: false
                    )
                }
            }
        }
    }

    private var onLogoutCallback: (() -> Unit)? = null
    private var onNavigateToBillingCallback: (() -> Unit)? = null

    fun setLogoutCallback(callback: () -> Unit) {
        onLogoutCallback = callback
    }

    fun setNavigateToBillingCallback(callback: () -> Unit) {
        onNavigateToBillingCallback = callback
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
}
