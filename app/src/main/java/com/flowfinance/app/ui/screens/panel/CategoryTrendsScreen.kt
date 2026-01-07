package com.flowfinance.app.ui.screens.panel

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.ui.components.CategoryHorizontalBarChart
import com.flowfinance.app.ui.components.CategoryPieChart
import com.flowfinance.app.ui.components.CategoryStackedAreaChart
import com.flowfinance.app.ui.components.CategoryTrendsLineChart
import com.flowfinance.app.ui.viewmodel.CategoryTrendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTrendsScreen(
    onBackClick: () -> Unit,
    onShowSheetClick: () -> Unit,
    onChartClick: (String) -> Unit,
    viewModel: CategoryTrendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Tendência por Categoria",
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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.monthlyData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Não há dados suficientes para exibir tendências por categoria.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // Espaço inicial para ajustar o padding top
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 1. Gráficos de Pizza (Carousel)
                item {
                    Text(
                        text = "Participação por Categoria",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Se não tiver dados mensais suficientes, mostre apenas o total
                    val pagerState = rememberPagerState(
                        initialPage = 0,
                        pageCount = { if (uiState.monthlyData.isNotEmpty()) 2 else 1 }
                    )
                    
                    Column {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            CategoryChartCard(title = if (page == 0) "Total Geral (Desde Sempre)" else "Mês Atual") {
                                if (page == 0) {
                                    CategoryPieChart(
                                        data = uiState.totalExpensesByCategory,
                                        currency = uiState.currency,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    // Pega o mês mais recente (último da lista ordenada)
                                    val currentMonthData = uiState.monthlyData.lastOrNull()?.categorySummaries ?: emptyList()
                                    if (currentMonthData.isNotEmpty()) {
                                        CategoryPieChart(
                                            data = currentMonthData,
                                            currency = uiState.currency,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Box(Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text("Sem dados para este mês")
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Indicador de página
                        if (pagerState.pageCount > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(pagerState.pageCount) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Ranking de Categorias (Barras Horizontais)
                item {
                    Text(
                        text = "Ranking de Gastos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var selectedRankingTab by remember { mutableIntStateOf(0) }
                    val tabs = listOf("Total", "Mensal") // Simplificado para Total e Mensal por enquanto, Semanal exigiria mais processamento
                    
                    Column {
                        TabRow(selectedTabIndex = selectedRankingTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedRankingTab == index,
                                    onClick = { selectedRankingTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                        
                        CategoryChartCard(title = "", modifier = Modifier.padding(top = 8.dp)) {
                            val dataToShow = if (selectedRankingTab == 0) {
                                uiState.totalExpensesByCategory.take(5) // Top 5
                            } else {
                                uiState.monthlyData.lastOrNull()?.categorySummaries?.take(5) ?: emptyList()
                            }
                            
                            if (dataToShow.isNotEmpty()) {
                                CategoryHorizontalBarChart(
                                    data = dataToShow,
                                    currency = uiState.currency,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Box(Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Sem dados para exibir")
                                }
                            }
                        }
                    }
                }

                // 3. Tendência ao Longo do Tempo (Linhas)
                item {
                    Text(
                        text = "Tendência Mensal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CategoryChartCard(
                        title = "Evolução por Categoria",
                        onClick = { onChartClick("category_trends_line") }
                    ) {
                        CategoryTrendsLineChart(
                            data = uiState.monthlyData,
                            categories = uiState.categories,
                            currency = uiState.currency,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            showTooltip = true
                        )
                    }
                }

                // 4. Área Empilhada (Contribuição Total)
                item {
                    Text(
                        text = "Composição dos Gastos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CategoryChartCard(
                        title = "Acumulado por Categoria",
                        onClick = { onChartClick("category_trends_stacked") }
                    ) {
                        CategoryStackedAreaChart(
                            data = uiState.monthlyData,
                            categories = uiState.categories,
                            currency = uiState.currency,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            showTooltip = true
                        )
                    }
                }

                // Botão Mostrar Planilha
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onShowSheetClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mostrar Planilha Detalhada")
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryChartCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            content()
        }
    }
}
