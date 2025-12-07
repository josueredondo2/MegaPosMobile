package com.devlosoft.megaposmobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devlosoft.megaposmobile.presentation.configuration.ConfigurationScreen
import com.devlosoft.megaposmobile.presentation.home.HomeScreen
import com.devlosoft.megaposmobile.presentation.login.LoginScreen
import com.devlosoft.megaposmobile.presentation.process.ProcessScreen

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

        composable(route = Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToProcess = { processType ->
                    navController.navigate(Screen.Process.createRoute(processType))
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
    }
}
