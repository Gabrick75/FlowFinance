package com.flowfinance.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.data.local.model.TransactionWithCategory
import com.flowfinance.app.ui.components.PieChart
import com.flowfinance.app.ui.screens.planning.rememberCategoryIcon
import com.flowfinance.app.ui.theme.GreenIncome
import com.flowfinance.app.ui.theme.RedExpense
import com.flowfinance.app.ui.viewmodel.DashboardViewModel
import com.flowfinance.app.util.TransactionType
import com.flowfinance.app.util.formatCurrency

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onSeeAllClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val chartData by viewModel.chartData.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BalanceCard(
                balance = uiState.totalBalance,
                income = uiState.monthlyIncome,
                expense = uiState.monthlyExpense,
                currencyCode = uiState.currency
            )
        }

        item {
            if (chartData.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Despesas por Categoria",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            PieChart(
                                data = chartData,
                                modifier = Modifier.size(150.dp)
                            )
                            // Legend could go here
                            Column {
                                chartData.forEach { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(it.category.color), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = it.category.name, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimas Transações",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSeeAllClick) {
                    Text("Ver todas")
                }
            }
        }

        items(uiState.recentTransactions) { transactionWithCategory ->
            TransactionItem(transactionWithCategory = transactionWithCategory, currencyCode = uiState.currency)
        }
    }
}

@Composable
fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double,
    currencyCode: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Saldo Total",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                text = formatCurrency(balance, currencyCode),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinanceIndicator(
                    label = "Receitas",
                    amount = income,
                    icon = Icons.Default.ArrowUpward,
                    color = GreenIncome,
                    onContainerColor = MaterialTheme.colorScheme.onPrimary,
                    currencyCode = currencyCode
                )
                FinanceIndicator(
                    label = "Despesas",
                    amount = expense,
                    icon = Icons.Default.ArrowDownward,
                    color = RedExpense,
                    onContainerColor = MaterialTheme.colorScheme.onPrimary,
                    currencyCode = currencyCode
                )
            }
        }
    }
}

@Composable
fun FinanceIndicator(
    label: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    onContainerColor: Color,
    currencyCode: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = onContainerColor.copy(alpha = 0.8f)
            )
            Text(
                text = formatCurrency(amount, currencyCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onContainerColor
            )
        }
    }
}

@Composable
fun TransactionItem(transactionWithCategory: TransactionWithCategory, currencyCode: String) {
    val transaction = transactionWithCategory.transaction
    val category = transactionWithCategory.category

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(category.color)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = rememberCategoryIcon(category.icon)
                    if(icon != null) {
                        Icon(icon, contentDescription = transaction.description, tint = Color.White)
                    } else {
                        Text(
                            text = category.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = transaction.date.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            val amountColor = if (transaction.type == TransactionType.INCOME) GreenIncome else RedExpense
            val prefix = if (transaction.type == TransactionType.INCOME) "+" else "-"
            
            Text(
                text = "$prefix${formatCurrency(transaction.amount, currencyCode)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
