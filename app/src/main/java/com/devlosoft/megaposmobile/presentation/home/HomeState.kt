package com.devlosoft.megaposmobile.presentation.home

import com.devlosoft.megaposmobile.domain.model.UserPermissions
import com.devlosoft.megaposmobile.presentation.shared.components.AuthorizationDialogState

data class HomeState(
    val userName: String = "",
    val currentDate: String = "",
    val terminalName: String = "",
    val stationStatus: String = "Cerrado",
    val isStationOpen: Boolean = false,
    val isLoading: Boolean = true,
    val showTodoDialog: Boolean = false,
    val todoDialogTitle: String = "",
    val showUserMenu: Boolean = false,
    val showLogoutConfirmDialog: Boolean = false,
    val isOpeningTerminal: Boolean = false,
    val openTerminalError: String? = null,
    val showOpenTerminalSuccessDialog: Boolean = false,
    val openTerminalMessage: String = "",
    val isClosingTerminal: Boolean = false,
    val closeTerminalError: String? = null,
    val showCloseTerminalSuccessDialog: Boolean = false,
    val closeTerminalMessage: String = "",
    val isCheckingPrinter: Boolean = false,
    val printerError: String? = null,
    // Permissions for menu items
    val canOpenTerminal: Boolean = false,
    val canCloseTerminal: Boolean = false,
    val canCloseDatafono: Boolean = false,
    val canBilling: Boolean = false,
    val canViewTransactions: Boolean = false,

    // User permissions for access validation
    val userPermissions: UserPermissions? = null,

    // Authorization dialog state
    val authorizationDialogState: AuthorizationDialogState = AuthorizationDialogState(),
    val pendingAuthorizationAction: HomePendingAction? = null
)

/**
 * Represents an action that requires authorization in HomeScreen
 */
sealed class HomePendingAction {
    data object OpenTerminal : HomePendingAction()
    data object CloseTerminal : HomePendingAction()
    data object CloseDatafono : HomePendingAction()
    data object Billing : HomePendingAction()
    data object ViewTransactions : HomePendingAction()
}
