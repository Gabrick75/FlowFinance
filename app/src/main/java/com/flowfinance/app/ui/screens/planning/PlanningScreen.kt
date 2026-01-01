package com.flowfinance.app.ui.screens.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.data.local.model.CategorySummary
import com.flowfinance.app.ui.screens.dashboard.formatCurrency
import com.flowfinance.app.ui.viewmodel.PlanningViewModel

@Composable
fun PlanningScreen(
    viewModel: PlanningViewModel = hiltViewModel()
) {
    val categorySpendings by viewModel.categorySpendings.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Metas por Categoria",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Acompanhe seus gastos mensais em relação ao planejado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(categorySpendings) { summary ->
            CategoryBudgetStart(summary)
        }
    }
}

@Composable
fun CategoryBudgetStart(summary: CategorySummary) {
    // Note: Since we don't have a way to edit budgets in the current requirements yet,
    // we will simulate a budget or use the optional one from entity if present.
    // Defaulting to a fixed value or logic for demo purposes if null.
    val budget = summary.category.budgetLimit ?: 1000.0 // Default demo budget
    val progress = (summary.totalAmount / budget).toFloat().coerceIn(0f, 1f)
    
    // Color changes as it approaches limit
    val progressColor = when {
        progress < 0.5f -> Color(0xFF4CAF50) // Green
        progress < 0.8f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = summary.category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${formatCurrency(summary.totalAmount)} / ${formatCurrency(budget)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
