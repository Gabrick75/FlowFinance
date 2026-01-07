package com.flowfinance.app.ui.screens.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PanelScreen(
    onNavigateToFinancialFlow: () -> Unit,
    onNavigateToPatternsAnalysis: () -> Unit,
    onNavigateToExpenseAnalysis: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Painel Financeiro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Acesse ferramentas avançadas de análise.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onNavigateToFinancialFlow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Fluxo Financeiro")
        }

        Button(
            onClick = onNavigateToPatternsAnalysis,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Tendência por Categoria")
        }

        Button(
            onClick = onNavigateToExpenseAnalysis,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Análise de Gastos")
        }

        Button(
            onClick = onNavigateToPatternsAnalysis,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Resumo Financeiro")
        }
        // Espaço para futuras funcionalidades do painel
    }
}
