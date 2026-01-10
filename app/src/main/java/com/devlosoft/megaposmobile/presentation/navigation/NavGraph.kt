package com.devlosoft.megaposmobile.presentation.navigation

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devlosoft.megaposmobile.presentation.advancedoptions.AdvancedOptionsScreen
import com.devlosoft.megaposmobile.presentation.billing.BillingScreen
import com.devlosoft.megaposmobile.presentation.billing.BillingViewModel
import com.devlosoft.megaposmobile.presentation.billing.TransactionScreen
import com.devlosoft.megaposmobile.presentation.configuration.ConfigurationScreen
import com.devlosoft.megaposmobile.presentation.home.HomeScreen
import com.devlosoft.megaposmobile.presentation.login.LoginScreen
import com.devlosoft.megaposmobile.presentation.process.ProcessScreen
import com.devlosoft.megaposmobile.presentation.process.ProcessViewModel
import com.devlosoft.megaposmobile.presentation.todaytransactions.TodayTransactionsScreen
import kotlin.reflect.typeOf

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: Any = Login
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Home) {
                        popUpTo<Login> { inclusive = true }
                    }
                },
                onNavigateToConfiguration = {
                    navController.navigate(Configuration)
                }
            )
        }

        composable<Configuration> {
            ConfigurationScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<AdvancedOptions> {
            AdvancedOptionsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Home> {
            HomeScreen(
                onLogout = {
                    navController.navigate(Login) {
                        popUpTo<Home> { inclusive = true }
                    }
                },
                onNavigateToProcess = { processType ->
                    navController.navigate(Process(processType))
                },
                onNavigateToBilling = {
                    navController.navigate(BillingGraph)
                },
                onNavigateToAdvancedOptions = {
                    navController.navigate(AdvancedOptions)
                },
                onNavigateToTodayTransactions = {
                    navController.navigate(TodayTransactions)
                }
            )
        }

        composable<TodayTransactions> {
            TodayTransactionsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Process> { backStackEntry ->
            val args = backStackEntry.toRoute<Process>()
            Log.d("NavGraph", "Process composable rendered - processType: ${args.processType}")

            ProcessScreen(
                processType = args.processType,
                onBack = {
                    Log.d("NavGraph", "=== onBack called for Process screen ===")
                    Log.d("NavGraph", "processType: ${args.processType}")
                    Log.d("NavGraph", "Current destination: ${navController.currentDestination?.route}")
                    try {
                        val popped = navController.popBackStack()
                        Log.d("NavGraph", "popBackStack result: $popped")
                        if (!popped) {
                            Log.w("NavGraph", "popBackStack returned false - backstack might be empty or already navigating")
                        }
                    } catch (e: Exception) {
                        Log.e("NavGraph", "ERROR during popBackStack for Process", e)
                    }
                }
            )
        }

        // Billing flow - nested navigation graph for sharing ViewModel
        navigation<BillingGraph>(startDestination = Billing()) {

            composable<Billing>(
                typeMap = mapOf(typeOf<Boolean>() to androidx.navigation.NavType.BoolType)
            ) { backStackEntry ->
                // Get ViewModel from parent graph to share between screens
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(BillingGraph)
                }
                val billingViewModel: BillingViewModel = hiltViewModel(parentEntry)

                BillingScreen(
                    viewModel = billingViewModel,
                    onNavigateToTransaction = {
                        navController.navigate(TransactionDetail)
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        navController.navigate(Login) {
                            popUpTo<Home> { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.popBackStack<Home>(inclusive = false)
                    }
                )
            }

            composable<TransactionDetail> { backStackEntry ->
                // Get ViewModel from parent graph (shared with BillingScreen)
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(BillingGraph)
                }
                val billingViewModel: BillingViewModel = hiltViewModel(parentEntry)

                TransactionScreen(
                    viewModel = billingViewModel,
                    onNavigateToPayment = { transactionId, amount ->
                        navController.navigate(PaymentProcess(transactionId, amount))
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        navController.navigate(Login) {
                            popUpTo<Home> { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.popBackStack<Home>(inclusive = false)
                    }
                )
            }
        }

        // Payment process screen
        composable<PaymentProcess> { backStackEntry ->
            val args = backStackEntry.toRoute<PaymentProcess>()
            val processViewModel: ProcessViewModel = hiltViewModel()

            // Start payment process when screen is launched
            androidx.compose.runtime.LaunchedEffect(args.transactionId, args.amount) {
                processViewModel.startPaymentProcess(args.transactionId, args.amount)
            }

            ProcessScreen(
                processType = "payment",
                viewModel = processViewModel,
                onBack = {
                    // Go back to TransactionDetail with items intact
                    navController.popBackStack()
                },
                onRetry = {
                    // Retry the payment with same parameters
                    processViewModel.startPaymentProcess(args.transactionId, args.amount)
                },
                onSuccess = {
                    // Navigate to Billing for new transaction, skipping recovery check
                    navController.navigate(Billing(skipRecoveryCheck = true)) {
                        popUpTo<Home> { inclusive = false }
                    }
                },
                onSkipPrintSuccess = {
                    // Navigate directly to new transaction when skipping print
                    navController.navigate(Billing(skipRecoveryCheck = true)) {
                        popUpTo<Home> { inclusive = false }
                    }
                },
                autoStartProcess = false,
                successButtonText = "Nueva transacción",
                errorBackButtonText = "Volver a la transacción"
            )
        }
    }
}
