package com.flowfinance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ExpenseAnalysisUiState(
    val isLoading: Boolean = false,
    val currency: String = "BRL",
    val averageDaily: Double = 0.0,
    val averageWeekly: Double = 0.0,
    val averageMonthly: Double = 0.0,
    val peakDayOfWeek: Pair<DayOfWeek, Double>? = null, // Dia da semana com maior média
    val peakDayOfMonth: Int? = null, // Dia do mês com maior frequência/valor
    val categoryMetrics: List<CategoryMetric> = emptyList(),
    val weeklyHeatmap: Map<DayOfWeek, Double> = emptyMap(), // Total gasto por dia da semana
    val dailyHistory: List<DailyExpense> = emptyList() // Para histograma
)

data class CategoryMetric(
    val categoryName: String,
    val color: Int,
    val transactionCount: Int,
    val totalAmount: Double,
    val averageTicket: Double,
    val isRecurring: Boolean // Simplificação: > X transações/mês ou frequência regular
)

data class DailyExpense(
    val date: LocalDate,
    val amount: Double
)

@HiltViewModel
class ExpenseAnalysisViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseAnalysisUiState(isLoading = true))
    val uiState: StateFlow<ExpenseAnalysisUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val userData = userPreferencesRepository.userData.stateIn(viewModelScope).value
            
            // Buscar todas as transações com detalhes da categoria
            val transactionsWithCategory = transactionRepository.getAllTransactionsWithCategory().first()
            
            // Filtrar apenas despesas
            val expenseTransactions = transactionsWithCategory.filter { 
                it.transaction.type == TransactionType.EXPENSE 
            }

            if (expenseTransactions.isEmpty()) {
                _uiState.value = ExpenseAnalysisUiState(isLoading = false, currency = userData.currency)
                return@launch
            }

            val transactions = expenseTransactions.map { it.transaction }
            val startDate = transactions.minOf { it.date }
            val endDate = transactions.maxOf { it.date }
            val daysDiff = ChronoUnit.DAYS.between(startDate, endDate) + 1
            val weeksDiff = (daysDiff / 7.0).coerceAtLeast(1.0)
            val monthsDiff = (daysDiff / 30.0).coerceAtLeast(1.0) // Aproximação

            val totalExpense = transactions.sumOf { it.amount }

            // 1. Médias
            val avgDaily = totalExpense / daysDiff
            val avgWeekly = totalExpense / weeksDiff
            val avgMonthly = totalExpense / monthsDiff

            // 2. Dias de maior gasto (Heatmap Semanal)
            val expensesByDayOfWeek = transactions.groupBy { it.date.dayOfWeek }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
            
            // Média por dia da semana (para não enviesar se houver mais segundas-feiras no período)
            // Mas para heatmap simples, soma total ou média simples serve. Vamos usar média por ocorrência desse dia.
            val countDayOfWeek = transactions.map { it.date }.distinct().groupBy { it.dayOfWeek }.mapValues { it.value.size }
            val avgExpensesByDayOfWeek = expensesByDayOfWeek.mapValues { (day, total) -> 
                total / (countDayOfWeek[day] ?: 1) 
            }
            
            val peakDayOfWeek = avgExpensesByDayOfWeek.maxByOrNull { it.value }?.toPair()

            // 3. Pico dia do mês
            val expensesByDayOfMonth = transactions.groupBy { it.date.dayOfMonth }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
            val peakDayOfMonth = expensesByDayOfMonth.maxByOrNull { it.value }?.key

            // 4. Métricas por Categoria
            val expensesByCategory = expenseTransactions.groupBy { it.category }
            val categoryMetrics = expensesByCategory.map { (category, list) ->
                val total = list.sumOf { it.transaction.amount }
                val count = list.size
                val ticket = total / count
                
                // Lógica simples para recorrente: se aparece em média mais de uma vez por mês
                val monthlyFrequency = count / monthsDiff
                val isRecurring = monthlyFrequency >= 1.0

                CategoryMetric(
                    categoryName = category.name,
                    color = category.color,
                    transactionCount = count,
                    totalAmount = total,
                    averageTicket = ticket,
                    isRecurring = isRecurring
                )
            }.sortedByDescending { it.totalAmount }

            // 5. Histórico Diário (Histograma)
            val dailyHistory = transactions.groupBy { it.date }
                .map { (date, list) -> DailyExpense(date, list.sumOf { it.amount }) }
                .sortedBy { it.date }

            _uiState.value = ExpenseAnalysisUiState(
                isLoading = false,
                currency = userData.currency,
                averageDaily = avgDaily,
                averageWeekly = avgWeekly,
                averageMonthly = avgMonthly,
                peakDayOfWeek = peakDayOfWeek,
                peakDayOfMonth = peakDayOfMonth,
                categoryMetrics = categoryMetrics,
                weeklyHeatmap = expensesByDayOfWeek, // Enviando soma total para heatmap visual
                dailyHistory = dailyHistory
            )
        }
    }
}
