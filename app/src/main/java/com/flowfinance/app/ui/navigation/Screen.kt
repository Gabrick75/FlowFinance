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
import com.flowfinance.app.R

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", R.string.nav_home, Icons.Default.Home)
    object Transactions : Screen("transactions", R.string.nav_history, Icons.AutoMirrored.Filled.List)
    object Planning : Screen("planning", R.string.nav_planning, Icons.Default.PieChart)
    object Panel : Screen("panel", R.string.nav_panel, Icons.Default.Analytics)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    
    // Sub-screens (not in bottom bar)
    object UserProfile : Screen("user_profile", R.string.nav_profile, Icons.Default.Settings)
    object ManageBudgets : Screen("manage_budgets", R.string.nav_manage_budgets, Icons.Default.Edit)
    object FinancialFlow : Screen("financial_flow", R.string.nav_financial_flow, Icons.Default.Timeline)
    object Sheet : Screen("sheet", R.string.nav_sheet, Icons.Default.TableChart)
    object CategoryTrends : Screen("category_trends", R.string.nav_category_trends, Icons.Default.Timeline)
    object CategoryTrendsSheet : Screen("category_trends_sheet", R.string.nav_trends_sheet, Icons.Default.TableChart)
    object ExpenseAnalysis : Screen("expense_analysis", R.string.nav_expense_analysis, Icons.Default.BarChart)
    object FinancialSummary : Screen("financial_summary", R.string.nav_financial_summary, Icons.Default.TableChart)
    object MonthlyHistory : Screen("monthly_history", R.string.nav_monthly_history, Icons.AutoMirrored.Filled.List)
    
    // Rota com argumento: "chart_detail/{chartType}"
    object ChartDetail : Screen("chart_detail/{chartType}", R.string.nav_chart_detail, Icons.Default.ShowChart) {
        fun createRoute(chartType: String) = "chart_detail/$chartType"
    }
}
