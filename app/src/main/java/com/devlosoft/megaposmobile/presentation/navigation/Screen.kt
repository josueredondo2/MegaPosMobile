package com.devlosoft.megaposmobile.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Transaction : Screen("transaction")
    data object Customer : Screen("customer/{identification}") {
        fun createRoute(identification: String) = "customer/$identification"
    }
}
