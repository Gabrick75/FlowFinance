package com.flowfinance.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
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
    object Sheet : Screen("sheet", "Planilha", Icons.Default.TableChart)
    object CategoryTrends : Screen("category_trends", "Tendência por Categoria", Icons.Default.Timeline)
    object CategoryTrendsSheet : Screen("category_trends_sheet", "Planilha de Tendências", Icons.Default.TableChart)
    object ExpenseAnalysis : Screen("expense_analysis", "Análise de Gastos", Icons.Default.BarChart)
    object FinancialSummary : Screen("financial_summary", "Resumo Financeiro", Icons.Default.TableChart)
    object MonthlyHistory : Screen("monthly_history", "Histórico Mensal", Icons.AutoMirrored.Filled.List)
    
    // Rota com argumento: "chart_detail/{chartType}"
    object ChartDetail : Screen("chart_detail/{chartType}", "Detalhe do Gráfico", Icons.Default.ShowChart) {
        fun createRoute(chartType: String) = "chart_detail/$chartType"
    }
}
