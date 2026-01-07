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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.ui.viewmodel.CategoryMetric
import com.flowfinance.app.ui.viewmodel.ExpenseAnalysisUiState
import com.flowfinance.app.ui.viewmodel.ExpenseAnalysisViewModel
import com.flowfinance.app.util.formatCurrency
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalysisScreen(
    onBackClick: () -> Unit,
    viewModel: ExpenseAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.expense_analysis_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                
                // Espaço inicial para ajustar o padding top
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 1. Resumo de Médias
                item {
                    Text(
                        text = stringResource(R.string.expense_avg_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AverageCard(
                            title = stringResource(R.string.expense_avg_daily),
                            amount = uiState.averageDaily,
                            currency = uiState.currency,
                            modifier = Modifier.weight(1f)
                        )
                        AverageCard(
                            title = stringResource(R.string.expense_avg_weekly),
                            amount = uiState.averageWeekly,
                            currency = uiState.currency,
                            modifier = Modifier.weight(1f)
                        )
                        AverageCard(
                            title = stringResource(R.string.expense_avg_monthly),
                            amount = uiState.averageMonthly,
                            currency = uiState.currency,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Dias de Maior Gasto (Picos)
                item {
                    Text(
                        text = stringResource(R.string.expense_peaks_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PeakCard(
                            title = stringResource(R.string.expense_peak_day_week),
                            value = uiState.peakDayOfWeek?.first?.getDisplayName(TextStyle.FULL, Locale.getDefault())?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "-",
                            subtitle = stringResource(R.string.expense_peak_day_week_sub)
                        )
                        PeakCard(
                            title = stringResource(R.string.expense_peak_day_month),
                            value = uiState.peakDayOfMonth?.toString() ?: "-",
                            subtitle = stringResource(R.string.expense_peak_day_month_sub)
                        )
                    }
                }

                // 3. Heatmap Semanal
                item {
                    Text(
                        text = stringResource(R.string.expense_heatmap_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.expense_heatmap_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyHeatmap(uiState.weeklyHeatmap)
                }

                // 4. Categorias (Recorrentes vs Ocasionais)
                item {
                    Text(
                        text = stringResource(R.string.expense_category_analysis),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryAnalysisSection(uiState)
                }
            }
        }
    }
}

@Composable
fun AverageCard(title: String, amount: Double, currency: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(amount, currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp // Slightly smaller to fit
            )
        }
    }
}

@Composable
fun PeakCard(title: String, value: String, subtitle: String) {
    Card(
        modifier = Modifier.width(160.dp), // Fixed width or weight
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun WeeklyHeatmap(data: Map<DayOfWeek, Double>) {
    if (data.isEmpty()) return
    
    val maxVal = data.values.maxOrNull() ?: 1.0
    val days = DayOfWeek.values()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                val amount = data[day] ?: 0.0
                val intensity = (amount / maxVal).toFloat().coerceIn(0.1f, 1f)
                val isPeak = amount == maxVal && amount > 0
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Bar
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(100.dp * intensity) // Max height 100dp
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (isPeak) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Label
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryAnalysisSection(uiState: ExpenseAnalysisUiState) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.expense_tab_recurring), stringResource(R.string.expense_tab_occasional))
    
    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val filteredList = if (selectedTab == 0) {
            uiState.categoryMetrics.filter { it.isRecurring }
        } else {
            uiState.categoryMetrics.filter { !it.isRecurring }
        }
        
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.expense_empty_category),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredList.forEach { metric ->
                    CategoryMetricItem(metric, uiState.currency)
                }
            }
        }
    }
}

@Composable
fun CategoryMetricItem(metric: CategoryMetric, currency: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(metric.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = metric.categoryName.take(1).uppercase(),
                    color = Color(metric.color),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.expense_transactions_count, metric.transactionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.expense_ticket_avg),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(metric.averageTicket, currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
