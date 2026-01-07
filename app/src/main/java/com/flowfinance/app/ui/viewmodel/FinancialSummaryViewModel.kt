package com.flowfinance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class FinancialSummaryUiState(
    val isLoading: Boolean = false,
    val currency: String = "BRL",
    val currentYear: Int = LocalDate.now().year,
    val totalSummary: SummaryData = SummaryData(),
    val yearlySummary: SummaryData = SummaryData(),
    val monthlySummary: SummaryData = SummaryData()
)

data class SummaryData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val remaining: Double = 0.0
)

@HiltViewModel
class FinancialSummaryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialSummaryUiState(isLoading = true))
    val uiState: StateFlow<FinancialSummaryUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val userData = userPreferencesRepository.userData.stateIn(viewModelScope).value
            
            // Get all transactions
            val allTransactions = transactionRepository.getAllTransactions().first()
            
            if (allTransactions.isEmpty()) {
                _uiState.value = FinancialSummaryUiState(isLoading = false, currency = userData.currency)
                return@launch
            }

            val currentDate = LocalDate.now()
            val currentYear = currentDate.year
            val currentMonth = YearMonth.from(currentDate)

            // 1. Total Atual (All Time)
            val totalSummary = calculateSummary(allTransactions)

            // 2. Total Anual (Current Year)
            val yearlyTransactions = allTransactions.filter { it.date.year == currentYear }
            val yearlySummary = calculateSummary(yearlyTransactions)

            // 3. Mês Atual
            val monthlyTransactions = allTransactions.filter { YearMonth.from(it.date) == currentMonth }
            val monthlySummary = calculateSummary(monthlyTransactions)

            _uiState.value = FinancialSummaryUiState(
                isLoading = false,
                currency = userData.currency,
                currentYear = currentYear,
                totalSummary = totalSummary,
                yearlySummary = yearlySummary,
                monthlySummary = monthlySummary
            )
        }
    }

    private fun calculateSummary(transactions: List<Transaction>): SummaryData {
        var income = 0.0
        var expense = 0.0

        transactions.forEach { transaction ->
            if (transaction.type == TransactionType.INCOME) {
                income += transaction.amount
            } else {
                expense += transaction.amount
            }
        }

        // Se o gasto for maior que a renda, o 'restante' pode ser negativo (dívida) ou zero se considerarmos apenas o que sobrou da renda.
        // Pela descrição "diferença entre o valor total e o gasto total", income - expense é o correto.
        val remaining = income - expense

        return SummaryData(
            totalIncome = income,
            totalExpense = expense,
            remaining = remaining
        )
    }
}
