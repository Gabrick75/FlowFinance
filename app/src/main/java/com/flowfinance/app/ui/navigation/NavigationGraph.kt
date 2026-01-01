package com.flowfinance.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            // DashboardScreen(navController)
        }
        composable(Screen.Transactions.route) {
            // TransactionsScreen(navController)
        }
        composable(Screen.Planning.route) {
            // PlanningScreen(navController)
        }
        composable(Screen.Settings.route) {
            // SettingsScreen(navController)
        }
    }
}
