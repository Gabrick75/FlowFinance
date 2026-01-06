package com.flowfinance.app.ui.screens.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.flowfinance.app.ui.components.ColorAccYield
import com.flowfinance.app.ui.components.ColorBalance
import com.flowfinance.app.ui.components.ColorSalary
import com.flowfinance.app.ui.components.ColorWealth
import com.flowfinance.app.ui.components.ColorYield
import com.flowfinance.app.ui.components.CombinedChart
import com.flowfinance.app.ui.components.GeneralOverviewChart
import com.flowfinance.app.ui.components.SalaryBarChart
import com.flowfinance.app.ui.components.YieldAreaChart
import com.flowfinance.app.ui.viewmodel.FinancialFlowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenChartScreen(
    chartType: String,
    onBackClick: () -> Unit,
    viewModel: FinancialFlowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val chartTitle = when (chartType) {
        "overview" -> "Visualização Geral"
        "salary" -> "Salário"
        "yield" -> "Rendimentos"
        "combined" -> "Salário X Rendimentos"
        else -> "Gráfico"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        chartTitle, 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Gráfico ocupando a maior parte do espaço
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (chartType) {
                            "overview" -> GeneralOverviewChart(
                                data = uiState.monthlyData,
                                modifier = Modifier.fillMaxSize()
                            )
                            "salary" -> SalaryBarChart(
                                data = uiState.monthlyData,
                                modifier = Modifier.fillMaxSize()
                            )
                            "yield" -> YieldAreaChart(
                                data = uiState.monthlyData,
                                modifier = Modifier.fillMaxSize()
                            )
                            "combined" -> CombinedChart(
                                data = uiState.monthlyData,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    // Legenda na parte inferior
                    ChartLegend(chartType)
                }
            }
        }
    }
}

@Composable
fun ChartLegend(chartType: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (chartType) {
            "overview" -> {
                Column {
                    LegendItem(ColorSalary, "Salário")
                    LegendItem(ColorYield, "Rend. Mensal")
                    LegendItem(ColorAccYield, "Rend. Acum.")
                }
                Column {
                    LegendItem(ColorBalance, "Saldo Acum.")
                    LegendItem(ColorWealth, "Patrimônio")
                }
            }
            "salary" -> LegendItem(ColorSalary, "Salário")
            "yield" -> {
                LegendItem(ColorAccYield, "Rend. Acum.")
                LegendItem(ColorYield, "Rend. Mensal")
            }
            "combined" -> {
                Column {
                    LegendItem(ColorSalary, "Salário (Col)")
                }
                Column {
                    LegendItem(ColorYield, "Rend. Mensal (Lin)")
                    LegendItem(ColorAccYield, "Rend. Acum. (Lin)")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
