package com.flowfinance.app.ui.screens.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.ui.components.GeneralOverviewChart
import com.flowfinance.app.ui.viewmodel.FinancialFlowViewModel
import com.flowfinance.app.ui.viewmodel.FinancialSummaryViewModel

@Composable
fun PanelScreen(
    onNavigateToFinancialFlow: () -> Unit,
    onNavigateToPatternsAnalysis: () -> Unit,
    onNavigateToExpenseAnalysis: () -> Unit,
    onNavigateToFinancialSummary: () -> Unit,
    financialFlowViewModel: FinancialFlowViewModel = hiltViewModel(),
    financialSummaryViewModel: FinancialSummaryViewModel = hiltViewModel()
) {
    val flowUiState by financialFlowViewModel.uiState.collectAsState()
    val summaryUiState by financialSummaryViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(scrollState)
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

        // Gráfico de Visualização Geral
        if (!flowUiState.isLoading && flowUiState.monthlyData.isNotEmpty()) {
            ChartCard(title = "Visualização Geral") {
                GeneralOverviewChart(
                    data = flowUiState.monthlyData,
                    currency = flowUiState.currency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // Resumo do Ano Atual
        if (!summaryUiState.isLoading) {
            SummaryTableCard(
                title = "Total Anual (${summaryUiState.currentYear})",
                data = summaryUiState.yearlySummary,
                currency = summaryUiState.currency
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botões de Navegação
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
            onClick = onNavigateToFinancialSummary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Resumo Financeiro")
        }
    }
}
