package com.flowfinance.app.ui.screens.panel

import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.graphics.applyCanvas
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
import com.flowfinance.app.util.saveBitmapToFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenChartScreen(
    chartType: String,
    onBackClick: () -> Unit,
    viewModel: FinancialFlowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var captureRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var showInfoPopup by remember { mutableStateOf(false) }

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
                        "Detalhes do Gráfico", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoPopup = !showInfoPopup }) {
                        Icon(Icons.Default.Info, contentDescription = "Informações da Legenda")
                    }
                    IconButton(onClick = { 
                        coroutineScope.launch {
                            val rect = captureRect ?: return@launch
                            
                            val bitmap = Bitmap.createBitmap(
                                view.width,
                                view.height,
                                Bitmap.Config.ARGB_8888
                            ).applyCanvas { 
                                view.draw(this)
                            }

                            val croppedBitmap = Bitmap.createBitmap(
                                bitmap,
                                rect.left.toInt(),
                                rect.top.toInt(),
                                rect.width.toInt(),
                                rect.height.toInt()
                            )
                            
                            val success = saveBitmapToFile(context, croppedBitmap, "chart_${System.currentTimeMillis()}.png")
                            
                            val message = if (success) "Gráfico salvo com sucesso!" else "Falha ao salvar gráfico."
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Baixar Gráfico")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { layoutCoordinates ->
                            captureRect = layoutCoordinates.boundsInWindow()
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            chartTitle, 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Gráfico ocupando a maior parte do espaço
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (chartType) {
                                "overview" -> GeneralOverviewChart(
                                    data = uiState.monthlyData,
                                    currency = uiState.currency,
                                    modifier = Modifier.fillMaxSize(),
                                    showTooltip = true
                                )
                                "salary" -> SalaryBarChart(
                                    data = uiState.monthlyData,
                                    currency = uiState.currency,
                                    modifier = Modifier.fillMaxSize(),
                                    showTooltip = true
                                )
                                "yield" -> YieldAreaChart(
                                    data = uiState.monthlyData,
                                    currency = uiState.currency,
                                    modifier = Modifier.fillMaxSize(),
                                    showTooltip = true
                                )
                                "combined" -> CombinedChart(
                                    data = uiState.monthlyData,
                                    currency = uiState.currency,
                                    modifier = Modifier.fillMaxSize(),
                                    showTooltip = true
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        // Legenda na parte inferior
                        ChartLegend(chartType)
                    }
                }
            }
            
            if (showInfoPopup) {
                LegendInfoPopup(chartType = chartType, onDismissRequest = { showInfoPopup = false })
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
                    LegendItem(ColorYield, "Rendimento Mensal")
                    LegendItem(ColorAccYield, "Rendimento Acumulado")
                }
                Column {
                    LegendItem(ColorBalance, "Saldo Acumulado")
                    LegendItem(ColorWealth, "Patrimônio")
                }
            }
            "salary" -> LegendItem(ColorSalary, "Salário")
            "yield" -> {
                LegendItem(ColorAccYield, "Rendimento Acumulado")
                LegendItem(ColorYield, "Rendimento Mensal")
            }
            "combined" -> {
                Column {
                    LegendItem(ColorSalary, "Salário")
                }
                Column {
                    LegendItem(ColorYield, "Rendimento Mensal")
                    LegendItem(ColorAccYield, "Rendimento Acumulado")
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

@Composable
private fun LegendInfoPopup(chartType: String, onDismissRequest: () -> Unit) {
    val explanations = getLegendExplanations(chartType)
    
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.9f),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Informações da Legenda",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                explanations.forEach { (title, desc) ->
                    Text(buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(title)
                        }
                        append(": ")
                        append(desc)
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private fun getLegendExplanations(chartType: String): Map<String, String> {
    return when (chartType) {
        "overview" -> mapOf(
            "Salário" to "Receitas (exceto Rendimentos).",
            "Rendimento Mensal" to "Ganhos de investimentos no mês.",
            "Rendimento Acumulado" to "Soma de todos os ganhos de investimentos.",
            "Saldo Acumulado" to "Diferença entre receitas e despesas acumulada.",
            "Patrimônio" to "Soma de todas as receitas."
        )
        "salary" -> mapOf(
            "Salário" to "Receitas (exceto Rendimentos)."
        )
        "yield" -> mapOf(
            "Rendimento Mensal" to "Ganhos de investimentos no mês.",
            "Rendimento Acumulado" to "Soma de todos os ganhos de investimentos."
        )
        "combined" -> mapOf(
            "Salário" to "Receitas (exceto Rendimentos).",
            "Rendimento Mensal" to "Ganhos de investimentos no mês.",
            "Rendimento Acumulado" to "Soma de todos os ganhos de investimentos."
        )
        else -> emptyMap()
    }
}
