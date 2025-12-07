package com.devlosoft.megaposmobile.presentation.home

data class HomeState(
    val userName: String = "",
    val currentDate: String = "",
    val terminalName: String = "",
    val stationStatus: String = "Cerrado",
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
    val closeTerminalMessage: String = ""
)
