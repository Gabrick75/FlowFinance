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
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class MonthlyHistoryUiState(
    val isLoading: Boolean = false,
    val currency: String = "BRL",
    val year: Int = LocalDate.now().year,
    val monthlyData: List<MonthSummary> = emptyList()
)

data class MonthSummary(
    val monthName: String,
    val summary: SummaryData
)

@HiltViewModel
class MonthlyHistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyHistoryUiState(isLoading = true))
    val uiState: StateFlow<MonthlyHistoryUiState> = _uiState

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val userData = userPreferencesRepository.userData.stateIn(viewModelScope).value
            val currentYear = LocalDate.now().year
            
            val allTransactions = transactionRepository.getAllTransactions().first()
            
            // Filter by current year
            val yearTransactions = allTransactions.filter { it.date.year == currentYear }
            
            // Group by month
            val transactionsByMonth = yearTransactions.groupBy { it.date.month }
            
            val monthlyList = mutableListOf<MonthSummary>()
            
            // Generate data for all 12 months
            for (month in Month.values()) {
                val transactions = transactionsByMonth[month] ?: emptyList()
                val summary = calculateSummary(transactions)
                
                val monthName = month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                
                monthlyList.add(MonthSummary(monthName, summary))
            }

            _uiState.value = MonthlyHistoryUiState(
                isLoading = false,
                currency = userData.currency,
                year = currentYear,
                monthlyData = monthlyList
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

        val remaining = income - expense

        return SummaryData(
            totalIncome = income,
            totalExpense = expense,
            remaining = remaining
        )
    }
}
