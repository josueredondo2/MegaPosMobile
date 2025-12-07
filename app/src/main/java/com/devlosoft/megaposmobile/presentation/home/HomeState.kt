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
    val showLogoutConfirmDialog: Boolean = false
)
