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

    // Authorization events - Request actions that may require authorization
    data object RequestOpenTerminal : HomeEvent()
    data object RequestCloseTerminal : HomeEvent()
    data object RequestCloseDatafono : HomeEvent()
    data object RequestBilling : HomeEvent()
    data object RequestViewTransactions : HomeEvent()
    data object RequestAdvancedOptions : HomeEvent()
    data class SubmitAuthorization(val userCode: String, val password: String) : HomeEvent()
    data object DismissAuthorizationDialog : HomeEvent()
    data object ClearAuthorizationError : HomeEvent()

    // Close Datafono events
    data object ShowCloseDatafonoConfirmDialog : HomeEvent()
    data object DismissCloseDatafonoConfirmDialog : HomeEvent()
    data object ConfirmCloseDatafono : HomeEvent()
    data object DismissCloseDatafonoError : HomeEvent()
    data object DismissCloseDatafonoSuccess : HomeEvent()
}
