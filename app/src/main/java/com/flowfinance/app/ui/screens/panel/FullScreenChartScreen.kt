package com.flowfinance.app.ui.screens.panel

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.graphics.applyCanvas
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.ui.components.CategoryStackedAreaChart
import com.flowfinance.app.ui.components.CategoryTrendsLineChart
import com.flowfinance.app.ui.components.ColorAccYield
import com.flowfinance.app.ui.components.ColorBalance
import com.flowfinance.app.ui.components.ColorSalary
import com.flowfinance.app.ui.components.ColorWealth
import com.flowfinance.app.ui.components.ColorYield
import com.flowfinance.app.ui.components.CombinedChart
import com.flowfinance.app.ui.components.GeneralOverviewChart
import com.flowfinance.app.ui.components.SalaryBarChart
import com.flowfinance.app.ui.components.YieldAreaChart
import com.flowfinance.app.ui.viewmodel.CategoryTrendsViewModel
import com.flowfinance.app.ui.viewmodel.FinancialFlowViewModel
import com.flowfinance.app.util.saveBitmapToFile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenChartScreen(
    chartType: String,
    onBackClick: () -> Unit
) {
    val isCategoryTrend = chartType == "category_trends_line" || chartType == "category_trends_stacked"
    
    if (isCategoryTrend) {
        CategoryTrendsFullScreen(chartType, onBackClick)
    } else {
        FinancialFlowFullScreen(chartType, onBackClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialFlowFullScreen(
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
        "overview" -> stringResource(R.string.chart_title_overview)
        "salary" -> stringResource(R.string.chart_title_salary)
        "yield" -> stringResource(R.string.chart_title_yield)
        "combined" -> stringResource(R.string.chart_title_combined)
        else -> "Gráfico"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.chart_detail_title), 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoPopup = !showInfoPopup }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.chart_info_legend))
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
                            
                            val message = if (success) context.getString(R.string.chart_save_success) else context.getString(R.string.chart_save_failure)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.chart_download))
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryTrendsFullScreen(
    chartType: String,
    onBackClick: () -> Unit,
    viewModel: CategoryTrendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var captureRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var showInfoPopup by remember { mutableStateOf(false) }

    val chartTitle = when (chartType) {
        "category_trends_line" -> stringResource(R.string.chart_title_category_line)
        "category_trends_stacked" -> stringResource(R.string.chart_title_category_stacked)
        else -> "Gráfico"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.chart_detail_title), 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoPopup = !showInfoPopup }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.chart_info_legend))
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
                            
                            val message = if (success) context.getString(R.string.chart_save_success) else context.getString(R.string.chart_save_failure)
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.chart_download))
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

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (chartType) {
                                "category_trends_line" -> CategoryTrendsLineChart(
                                    data = uiState.monthlyData,
                                    categories = uiState.categories,
                                    currency = uiState.currency,
                                    modifier = Modifier.fillMaxSize(),
                                    showTooltip = true
                                )
                                "category_trends_stacked" -> CategoryStackedAreaChart(
                                    data = uiState.monthlyData,
                                    categories = uiState.categories,
                                    currency = uiState.currency,
                                    modifier = Modifier.fillMaxSize(),
                                    showTooltip = true
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(stringResource(R.string.chart_categories_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3
                        ) {
                             uiState.categories.forEach { category ->
                                 LegendItem(Color(category.color), category.name)
                                 Spacer(modifier = Modifier.width(8.dp))
                             }
                        }
                    }
                }
            }
            
            if (showInfoPopup) {
                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = { showInfoPopup = false }
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(0.9f),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.chart_info_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                            
                            val desc = if (chartType == "category_trends_line") 
                                "Este gráfico mostra a variação dos gastos de cada categoria mês a mês, permitindo identificar tendências de aumento ou redução."
                            else 
                                "Este gráfico exibe como cada categoria contribui para o total de despesas mensais, ajudando a visualizar a composição do seu orçamento."
                                
                            Text(desc)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.chart_legend_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column {
                                uiState.categories.forEach { category ->
                                    LegendItem(Color(category.color), category.name)
                                }
                            }
                        }
                    }
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
                    LegendItem(ColorSalary, stringResource(R.string.sheet_col_salary))
                    LegendItem(ColorYield, stringResource(R.string.sheet_col_yield_monthly))
                    LegendItem(ColorAccYield, stringResource(R.string.sheet_col_yield_acc))
                }
                Column {
                    LegendItem(ColorBalance, stringResource(R.string.sheet_col_balance_acc))
                    LegendItem(ColorWealth, stringResource(R.string.sheet_col_wealth))
                }
            }
            "salary" -> LegendItem(ColorSalary, stringResource(R.string.sheet_col_salary))
            "yield" -> {
                LegendItem(ColorAccYield, stringResource(R.string.sheet_col_yield_acc))
                LegendItem(ColorYield, stringResource(R.string.sheet_col_yield_monthly))
            }
            "combined" -> {
                Column {
                    LegendItem(ColorSalary, stringResource(R.string.sheet_col_salary))
                }
                Column {
                    LegendItem(ColorYield, stringResource(R.string.sheet_col_yield_monthly))
                    LegendItem(ColorAccYield, stringResource(R.string.sheet_col_yield_acc))
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
                    text = stringResource(R.string.chart_info_legend),
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

@Composable
private fun getLegendExplanations(chartType: String): Map<String, String> {
    // Note: Since this is composable function returning a map, we can use stringResource.
    // However, map keys are strings here. 
    // Let's manually construct the map with string resources.
    
    return when (chartType) {
        "overview" -> mapOf(
            stringResource(R.string.sheet_col_salary) to stringResource(R.string.summary_info_total), // Reusing existing or need new strings?
            // Let's stick to what was hardcoded if we don't have exact matches, or add new strings.
            // But for now, let's keep hardcoded strings in getLegendExplanations OR better, create a composable way to render it directly without map return.
            // Or just fetch strings here.
            
             // "Salário" to "Receitas (exceto Rendimentos)."
             stringResource(R.string.sheet_col_salary) to "Receitas (exceto Rendimentos).",
             stringResource(R.string.sheet_col_yield_monthly) to "Ganhos de investimentos no mês.",
             stringResource(R.string.sheet_col_yield_acc) to "Soma de todos os ganhos de investimentos.",
             stringResource(R.string.sheet_col_balance_acc) to "Diferença entre receitas e despesas acumulada.",
             stringResource(R.string.sheet_col_wealth) to "Soma de todas as receitas."
        )
        "salary" -> mapOf(
            stringResource(R.string.sheet_col_salary) to "Receitas (exceto Rendimentos)."
        )
        "yield" -> mapOf(
            stringResource(R.string.sheet_col_yield_monthly) to "Ganhos de investimentos no mês.",
            stringResource(R.string.sheet_col_yield_acc) to "Soma de todos os ganhos de investimentos."
        )
        "combined" -> mapOf(
             stringResource(R.string.sheet_col_salary) to "Receitas (exceto Rendimentos).",
             stringResource(R.string.sheet_col_yield_monthly) to "Ganhos de investimentos no mês.",
             stringResource(R.string.sheet_col_yield_acc) to "Soma de todos os ganhos de investimentos."
        )
        else -> emptyMap()
    }
}
