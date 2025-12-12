package com.devlosoft.megaposmobile.presentation.home

sealed class HomeEvent {
    data object OpenTerminal : HomeEvent()
    data object CloseTerminal : HomeEvent()
    data object Billing : HomeEvent()
    data object CheckPrinterAndNavigateToBilling : HomeEvent()
    data object DismissPrinterError : HomeEvent()
    data object DailyTransactions : HomeEvent()
    data object DismissDialog : HomeEvent()
    data object ToggleUserMenu : HomeEvent()
    data object DismissUserMenu : HomeEvent()
    data object ShowLogoutConfirmDialog : HomeEvent()
    data object DismissLogoutConfirmDialog : HomeEvent()
    data object ConfirmLogout : HomeEvent()
    data object DismissOpenTerminalError : HomeEvent()
    data object DismissOpenTerminalSuccess : HomeEvent()
    data object DismissCloseTerminalError : HomeEvent()
    data object DismissCloseTerminalSuccess : HomeEvent()
}
