package com.flowfinance.app.ui.screens.panel

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.ui.viewmodel.FinancialSummaryViewModel
import com.flowfinance.app.ui.viewmodel.SummaryData
import com.flowfinance.app.util.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSummaryScreen(
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: FinancialSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInfoPopup by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.summary_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoPopup = !showInfoPopup }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(id = R.string.summary_info_title))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
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

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        SummaryTableCard(
                            title = stringResource(id = R.string.summary_total_current),
                            data = uiState.totalSummary,
                            currency = uiState.currency
                        )
                    }

                    item {
                        SummaryTableCard(
                            title = stringResource(id = R.string.summary_total_annual, uiState.currentYear),
                            data = uiState.yearlySummary,
                            currency = uiState.currency
                        )
                    }

                    item {
                        SummaryTableCard(
                            title = stringResource(id = R.string.summary_current_month),
                            data = uiState.monthlySummary,
                            currency = uiState.currency
                        )
                    }

                    item {
                        Button(
                            onClick = onHistoryClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.summary_view_history))
                        }
                    }
                }
            }

            if (showInfoPopup) {
                Popup(
                    alignment = Alignment.TopEnd,
                    onDismissRequest = { showInfoPopup = false }
                ) {
                    Card(
                        modifier = Modifier
                            .padding(top = 60.dp, end = 16.dp)
                            .fillMaxWidth(0.8f),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(id = R.string.summary_info_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(id = R.string.summary_info_total),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(id = R.string.summary_info_spent),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(id = R.string.summary_info_remaining),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryTableCard(
    title: String,
    data: SummaryData,
    currency: String
) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(id = R.string.summary_col_category), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text(stringResource(id = R.string.summary_col_percentage), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.8f))
                Text(stringResource(id = R.string.summary_col_value), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Rows
            SummaryRow(stringResource(id = R.string.summary_row_total), 100.0, data.totalIncome, currency, Color.Unspecified)
            SummaryRow(
                stringResource(id = R.string.summary_row_spent),
                if (data.totalIncome > 0) (data.totalExpense / data.totalIncome) * 100 else 0.0,
                -data.totalExpense, // Display as negative
                currency,
                Color.Red
            )
            SummaryRow(
                stringResource(id = R.string.summary_row_remaining),
                if (data.totalIncome > 0) (data.remaining / data.totalIncome) * 100 else 0.0,
                data.remaining,
                currency,
                Color(0xFF4CAF50) // Green
            )
        }
    }
}

@Composable
fun SummaryRow(category: String, percentage: Double, value: Double, currency: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "${String.format("%.1f", percentage)}%",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.8f)
        )
        Text(
            formatCurrency(value, currency),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
