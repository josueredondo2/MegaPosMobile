package com.devlosoft.megaposmobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.devlosoft.megaposmobile.presentation.advancedoptions.AdvancedOptionsScreen
import com.devlosoft.megaposmobile.presentation.billing.BillingScreen
import com.devlosoft.megaposmobile.presentation.billing.BillingViewModel
import com.devlosoft.megaposmobile.presentation.billing.TransactionScreen
import com.devlosoft.megaposmobile.presentation.configuration.ConfigurationScreen
import com.devlosoft.megaposmobile.presentation.home.HomeScreen
import com.devlosoft.megaposmobile.presentation.login.LoginScreen
import com.devlosoft.megaposmobile.presentation.process.ProcessScreen
import com.devlosoft.megaposmobile.presentation.process.ProcessViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToConfiguration = {
                    navController.navigate(Screen.Configuration.route)
                }
            )
        }

        composable(route = Screen.Configuration.route) {
            ConfigurationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.AdvancedOptions.route) {
            AdvancedOptionsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToProcess = { processType ->
                    navController.navigate(Screen.Process.createRoute(processType))
                },
                onNavigateToBilling = {
                    navController.navigate(Screen.Billing.route)
                },
                onNavigateToAdvancedOptions = {
                    navController.navigate(Screen.AdvancedOptions.route)
                }
            )
        }

        composable(
            route = Screen.Process.route,
            arguments = listOf(
                navArgument("processType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val processType = backStackEntry.arguments?.getString("processType") ?: ""
            ProcessScreen(
                processType = processType,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // Billing flow - share ViewModel between screens
        composable(route = Screen.Billing.route) { backStackEntry ->
            // Use the backStackEntry parameter directly instead of getBackStackEntry
            // to avoid crashes during navigation transitions
            val billingViewModel: BillingViewModel = hiltViewModel(backStackEntry)

            BillingScreen(
                viewModel = billingViewModel,
                onNavigateToTransaction = {
                    navController.navigate(Screen.TransactionDetail.route)
                },
                onBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        composable(route = Screen.TransactionDetail.route) {
            // Get the ViewModel from the billing backstack entry to share state
            // Cache the entry to avoid issues during recomposition when navigating away
            val billingEntry = remember {
                try {
                    navController.getBackStackEntry(Screen.Billing.route)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

            // If billing entry doesn't exist, navigate back
            if (billingEntry == null) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.navigate(Screen.Billing.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
                return@composable
            }

            val billingViewModel: BillingViewModel = hiltViewModel(billingEntry)

            TransactionScreen(
                viewModel = billingViewModel,
                onNavigateToPayment = { transactionId, amount ->
                    navController.navigate(Screen.PaymentProcess.createRoute(transactionId, amount))
                },
                onBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        // Payment process screen
        composable(
            route = Screen.PaymentProcess.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            val amount = backStackEntry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
            val processViewModel: ProcessViewModel = hiltViewModel()

            // Start payment process when screen is launched
            androidx.compose.runtime.LaunchedEffect(transactionId, amount) {
                processViewModel.startPaymentProcess(transactionId, amount)
            }

            ProcessScreen(
                processType = "payment",
                viewModel = processViewModel,
                onBack = {
                    // Navigate back to Billing screen for a new transaction
                    navController.navigate(Screen.Billing.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                autoStartProcess = false
            )
        }
    }
}
