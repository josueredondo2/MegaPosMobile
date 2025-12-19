package com.devlosoft.megaposmobile.presentation.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Configuration : Screen("configuration")
    data object AdvancedOptions : Screen("advanced-options")
    data object Billing : Screen("billing?skipRecoveryCheck={skipRecoveryCheck}") {
        fun createRoute(skipRecoveryCheck: Boolean = false) = "billing?skipRecoveryCheck=$skipRecoveryCheck"
    }
    data object TransactionDetail : Screen("transaction-detail")
    data object Customer : Screen("customer/{identification}") {
        fun createRoute(identification: String) = "customer/$identification"
    }
    data object Process : Screen("process/{processType}") {
        fun createRoute(processType: String) = "process/$processType"
    }
    data object PaymentProcess : Screen("payment-process/{transactionId}/{amount}") {
        fun createRoute(transactionId: String, amount: Double) = "payment-process/$transactionId/$amount"
    }
}
