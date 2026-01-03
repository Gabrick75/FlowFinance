package com.flowfinance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.flowfinance.app.ui.components.AddTransactionSheet
import com.flowfinance.app.ui.navigation.Screen
import com.flowfinance.app.ui.screens.dashboard.DashboardScreen
import com.flowfinance.app.ui.screens.planning.ManageBudgetsScreen
import com.flowfinance.app.ui.screens.planning.PlanningScreen
import com.flowfinance.app.ui.screens.settings.SettingsScreen
import com.flowfinance.app.ui.screens.settings.UserProfileScreen
import com.flowfinance.app.ui.screens.transactions.TransactionsScreen
import com.flowfinance.app.ui.theme.FlowFinanceTheme
import com.flowfinance.app.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val userData by settingsViewModel.userData.collectAsState()
            
            val isDarkTheme = userData.isDarkTheme

            FlowFinanceTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                var showBottomSheet by remember { mutableStateOf(false) }
                val sheetState = rememberModalBottomSheetState()
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute != Screen.UserProfile.route && currentRoute != Screen.ManageBudgets.route) {
                            NavigationBar {
                                val currentDestination = navBackStackEntry?.destination
                                val screens = listOf(
                                    Screen.Dashboard,
                                    Screen.Transactions,
                                    Screen.Planning,
                                    Screen.Settings
                                )
                                screens.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = null) },
                                        label = { Text(screen.title) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        when (currentRoute) {
                            Screen.Dashboard.route -> {
                                FloatingActionButton(
                                    onClick = { showBottomSheet = true },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Nova Transação")
                                }
                            }
                            Screen.Planning.route -> {
                                FloatingActionButton(
                                    onClick = { navController.navigate(Screen.ManageBudgets.route) },
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Gerenciar Metas")
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Dashboard.route
                        ) {
                            composable(Screen.Dashboard.route) {
                                DashboardScreen(onSeeAllClick = {
                                    navController.navigate(Screen.Transactions.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                })
                            }
                            composable(Screen.Transactions.route) {
                                TransactionsScreen()
                            }
                            composable(Screen.Planning.route) {
                                PlanningScreen()
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    onProfileClick = { navController.navigate(Screen.UserProfile.route) }
                                )
                            }
                            composable(Screen.UserProfile.route) {
                                UserProfileScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable(Screen.ManageBudgets.route) {
                                ManageBudgetsScreen(onBackClick = { navController.popBackStack() })
                            }
                        }
                    }

                    if (showBottomSheet) {
                        AddTransactionSheet(
                            onDismiss = { showBottomSheet = false },
                            sheetState = sheetState
                        )
                    }
                }
            }
        }
    }
}
