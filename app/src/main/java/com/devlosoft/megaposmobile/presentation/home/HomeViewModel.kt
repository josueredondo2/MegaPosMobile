package com.devlosoft.megaposmobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devlosoft.megaposmobile.core.state.StationStatus
import com.devlosoft.megaposmobile.data.local.dao.ServerConfigDao
import com.devlosoft.megaposmobile.data.local.preferences.SessionManager
import com.devlosoft.megaposmobile.domain.usecase.LogoutUseCase
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
    private val sessionManager: SessionManager,
    private val serverConfigDao: ServerConfigDao,
    private val stationStatus: StationStatus
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

            // Get station status
            stationStatus.state.collect { status ->
                _state.update {
                    it.copy(
                        userName = userName,
                        currentDate = currentDate,
                        terminalName = terminalName,
                        stationStatus = stationStatus.getDisplayText(),
                        isLoading = false
                    )
                }
            }
        }
    }

    private var onLogoutCallback: (() -> Unit)? = null

    fun setLogoutCallback(callback: () -> Unit) {
        onLogoutCallback = callback
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OpenTerminal -> {
                showTodoDialog("Aperturar Terminal")
            }
            is HomeEvent.CloseTerminal -> {
                showTodoDialog("Cierre Terminal")
            }
            is HomeEvent.Billing -> {
                showTodoDialog("Facturación")
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
        }
    }

    private fun showTodoDialog(title: String) {
        _state.update { it.copy(showTodoDialog = true, todoDialogTitle = title) }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase().collect { }
            onLogoutCallback?.invoke()
        }
    }
}
