package com.flowfinance.app.ui.screens.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.data.local.model.TransactionWithCategory
import com.flowfinance.app.ui.screens.dashboard.TransactionItem
import com.flowfinance.app.util.formatCurrency
import com.flowfinance.app.ui.viewmodel.TransactionsViewModel
import com.flowfinance.app.util.TransactionType
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            ) {
                Text(
                    text = "Histórico",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Visualize e gerencie suas transações.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Month Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }
                    Text(
                        text = uiState.currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR")))
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Buscar transação...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            uiState.transactionsByDate.forEach { (date, transactions) ->
                stickyHeader {
                    DateHeader(date = date, dailyTotal = transactions.sumOf { 
                         if (it.transaction.type == TransactionType.EXPENSE) it.transaction.amount else 0.0
                    }, currencyCode = uiState.currency)
                }
                
                items(
                    items = transactions,
                    key = { it.transaction.id } 
                ) { transactionWithCategory ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteTransaction(transactionWithCategory.transaction)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                                    else -> Color.Transparent
                                }
                            )
                            val scale by animateFloatAsState(
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f
                            )

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.scale(scale),
                                    tint = Color.White
                                )
                            }
                        },
                        content = {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).background(MaterialTheme.colorScheme.background)) {
                                TransactionItem(transactionWithCategory = transactionWithCategory, currencyCode = uiState.currency)
                            }
                        },
                        enableDismissFromStartToEnd = false
                    )
                }
            }
            
            if (uiState.transactionsByDate.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                         Text(
                             text = "Nenhuma transação encontrada",
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: java.time.LocalDate, dailyTotal: Double, currencyCode: String = "BRL") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("dd 'de' MMM, EEEE", Locale("pt", "BR"))),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (dailyTotal > 0) {
            Text(
                text = "Gasto: ${formatCurrency(dailyTotal, currencyCode)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
