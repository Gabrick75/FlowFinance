package com.flowfinance.app.ui.screens.panel

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.ui.components.CombinedChart
import com.flowfinance.app.ui.components.GeneralOverviewChart
import com.flowfinance.app.ui.components.SalaryBarChart
import com.flowfinance.app.ui.components.YieldAreaChart
import com.flowfinance.app.ui.screens.settings.shareFile
import com.flowfinance.app.ui.viewmodel.FinancialFlowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialFlowScreen(
    onBackClick: () -> Unit,
    onShowSheetClick: () -> Unit,
    onChartClick: (String) -> Unit, // Callback for chart clicks
    viewModel: FinancialFlowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Fluxo Financeiro",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opções")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Baixar CSV") },
                            onClick = {
                                showMenu = false
                                viewModel.exportSheetToCsv { path ->
                                    if (path != null) {
                                        shareFile(context, path)
                                    } else {
                                        Toast.makeText(context, "Erro ao exportar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // 1. Visualização Geral
                item {
                    ChartCard(
                        title = "Visualização Geral",
                        onClick = { onChartClick("overview") }
                    ) {
                        GeneralOverviewChart(
                            data = uiState.monthlyData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                // 2. Salário e Rendimentos (Lado a Lado)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ChartCard(
                                title = "Salário",
                                onClick = { onChartClick("salary") }
                            ) {
                                SalaryBarChart(
                                    data = uiState.monthlyData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ChartCard(
                                title = "Rendimentos",
                                onClick = { onChartClick("yield") }
                            ) {
                                YieldAreaChart(
                                    data = uiState.monthlyData,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Salário X Rendimentos
                item {
                    ChartCard(
                        title = "Salário X Rendimentos",
                        onClick = { onChartClick("combined") }
                    ) {
                        CombinedChart(
                            data = uiState.monthlyData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }

                // 4. Botão Mostrar Planilha
                item {
                    Button(
                        onClick = onShowSheetClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mostrar Planilha")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}, // Added onClick parameter
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable { onClick() } // Make card clickable
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
