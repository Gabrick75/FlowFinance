package com.flowfinance.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Início", Icons.Default.Home)
    object Transactions : Screen("transactions", "Histórico", Icons.AutoMirrored.Filled.List)
    object Planning : Screen("planning", "Planejamento", Icons.Default.PieChart)
    object Settings : Screen("settings", "Configurações", Icons.Default.Settings)
}
