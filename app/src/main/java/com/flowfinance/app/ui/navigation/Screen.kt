package com.flowfinance.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Início", Icons.Default.Home)
    object Transactions : Screen("transactions", "Histórico", Icons.AutoMirrored.Filled.List)
    object Planning : Screen("planning", "Planejamento", Icons.Default.PieChart)
    object Panel : Screen("panel", "Painel", Icons.Default.Analytics)
    object Settings : Screen("settings", "Configurações", Icons.Default.Settings)
    
    // Sub-screens (not in bottom bar)
    object UserProfile : Screen("user_profile", "Perfil", Icons.Default.Settings)
    object ManageBudgets : Screen("manage_budgets", "Gerenciar Metas", Icons.Default.Edit)
    object FinancialFlow : Screen("financial_flow", "Fluxo Financeiro", Icons.Default.Timeline)
}
