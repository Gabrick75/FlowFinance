package com.flowfinance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowfinance.app.ui.components.AddTransactionSheet
import com.flowfinance.app.ui.navigation.Screen
import com.flowfinance.app.ui.screens.dashboard.DashboardScreen
import com.flowfinance.app.ui.screens.panel.CategoryTrendsScreen
import com.flowfinance.app.ui.screens.panel.CategoryTrendsSheetScreen
import com.flowfinance.app.ui.screens.panel.ExpenseAnalysisScreen
import com.flowfinance.app.ui.screens.panel.FinancialFlowScreen
import com.flowfinance.app.ui.screens.panel.FullScreenChartScreen
import com.flowfinance.app.ui.screens.panel.PanelScreen
import com.flowfinance.app.ui.screens.panel.SheetScreen
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
                        if (currentRoute != Screen.UserProfile.route && 
                            currentRoute != Screen.ManageBudgets.route &&
                            currentRoute != Screen.FinancialFlow.route &&
                            currentRoute != Screen.Sheet.route &&
                            currentRoute != Screen.CategoryTrends.route &&
                            currentRoute != Screen.CategoryTrendsSheet.route &&
                            currentRoute != Screen.ExpenseAnalysis.route &&
                            currentRoute?.startsWith("chart_detail") != true) {
                            NavigationBar {
                                val currentDestination = navBackStackEntry?.destination
                                val screens = listOf(
                                    Screen.Dashboard,
                                    Screen.Transactions,
                                    Screen.Planning,
                                    Screen.Panel,
                                    Screen.Settings
                                )
                                screens.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                                        label = null,
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
                            composable(Screen.Panel.route) {
                                PanelScreen(
                                    onNavigateToFinancialFlow = { navController.navigate(Screen.FinancialFlow.route) },
                                    onNavigateToPatternsAnalysis = { navController.navigate(Screen.CategoryTrends.route) },
                                    onNavigateToExpenseAnalysis = { navController.navigate(Screen.ExpenseAnalysis.route) }
                                )
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
                            composable(Screen.FinancialFlow.route) {
                                FinancialFlowScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onShowSheetClick = { navController.navigate(Screen.Sheet.route) },
                                    onChartClick = { chartType -> 
                                        navController.navigate(Screen.ChartDetail.createRoute(chartType))
                                    }
                                )
                            }
                            composable(Screen.Sheet.route) {
                                SheetScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable(Screen.CategoryTrends.route) {
                                CategoryTrendsScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onShowSheetClick = { navController.navigate(Screen.CategoryTrendsSheet.route) },
                                    onChartClick = { chartType ->
                                        navController.navigate(Screen.ChartDetail.createRoute(chartType))
                                    }
                                )
                            }
                            composable(Screen.CategoryTrendsSheet.route) {
                                CategoryTrendsSheetScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable(Screen.ExpenseAnalysis.route) {
                                ExpenseAnalysisScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable(
                                route = Screen.ChartDetail.route,
                                arguments = listOf(navArgument("chartType") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val chartType = backStackEntry.arguments?.getString("chartType") ?: "overview"
                                FullScreenChartScreen(
                                    chartType = chartType,
                                    onBackClick = { navController.popBackStack() }
                                )
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
