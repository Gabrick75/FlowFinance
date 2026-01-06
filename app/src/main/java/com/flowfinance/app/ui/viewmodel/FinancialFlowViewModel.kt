package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.model.TransactionWithCategory
import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class MonthlyFinancialData(
    val yearMonth: YearMonth,
    val salary: Double = 0.0,
    val monthlyYield: Double = 0.0,
    val accumulatedYield: Double = 0.0,
    val accumulatedBalance: Double = 0.0,
    val totalWealth: Double = 0.0
)

data class FinancialFlowUiState(
    val monthlyData: List<MonthlyFinancialData> = emptyList(),
    val currency: String = "BRL",
    val isLoading: Boolean = true
)

@HiltViewModel
class FinancialFlowViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<FinancialFlowUiState> = combine(
        transactionRepository.getAllTransactionsWithCategory(),
        userPreferencesRepository.userData
    ) { transactions, userData ->
        val processedData = processTransactions(transactions)
        FinancialFlowUiState(
            monthlyData = processedData,
            currency = userData.currency,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialFlowUiState()
    )

    private fun processTransactions(transactions: List<TransactionWithCategory>): List<MonthlyFinancialData> {
        if (transactions.isEmpty()) return emptyList()

        val sortedTransactions = transactions.sortedBy { it.transaction.date }
        val startMonth = YearMonth.from(sortedTransactions.first().transaction.date)
        val endMonth = YearMonth.now()
        
        val result = mutableListOf<MonthlyFinancialData>()
        var currentMonth = startMonth

        var runningAccumulatedYield = 0.0
        var runningAccumulatedBalance = 0.0
        var runningTotalWealth = 0.0

        while (!currentMonth.isAfter(endMonth)) {
            val monthTransactions = sortedTransactions.filter { 
                YearMonth.from(it.transaction.date) == currentMonth 
            }

            // "Salário" agora inclui todas as receitas, exceto "Rendimentos"
            val salary = monthTransactions
                .filter { it.transaction.type == TransactionType.INCOME && !it.category.name.equals("Rendimentos", ignoreCase = true) }
                .sumOf { it.transaction.amount }

            val monthlyYield = monthTransactions
                .filter { it.category.name.equals("Rendimentos", ignoreCase = true) && it.transaction.type == TransactionType.INCOME }
                .sumOf { it.transaction.amount }

            val totalIncomeOfMonth = salary + monthlyYield

            val totalExpenseOfMonth = monthTransactions
                .filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.transaction.amount }

            runningAccumulatedYield += monthlyYield
            runningAccumulatedBalance += (totalIncomeOfMonth - totalExpenseOfMonth)
            runningTotalWealth += totalIncomeOfMonth

            result.add(
                MonthlyFinancialData(
                    yearMonth = currentMonth,
                    salary = salary,
                    monthlyYield = monthlyYield,
                    accumulatedYield = runningAccumulatedYield,
                    accumulatedBalance = runningAccumulatedBalance,
                    totalWealth = runningTotalWealth
                )
            )

            currentMonth = currentMonth.plusMonths(1)
        }

        return result
    }

    fun exportSheetToCsv(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val data = uiState.value.monthlyData
                val currency = uiState.value.currency
                
                val fileName = "flowfinance_fluxo_${System.currentTimeMillis()}.csv"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                FileWriter(file).use { writer ->
                    writer.append("Data;Salário;Rendimento Mensal;Rendimento Acumulado;Saldo Acumulado;Patrimônio Total\n")
                    
                    data.forEach { item ->
                        val dateStr = item.yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("pt", "BR")))
                        writer.append("$dateStr;${item.salary};${item.monthlyYield};${item.accumulatedYield};${item.accumulatedBalance};${item.totalWealth}\n")
                    }
                }
                
                onResult(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }
}
