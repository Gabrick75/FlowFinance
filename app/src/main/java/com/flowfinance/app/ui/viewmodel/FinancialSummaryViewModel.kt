package com.flowfinance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.preferences.UserData
import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    val uiState: StateFlow<FinancialSummaryUiState> = combine(
        transactionRepository.getAllTransactions(),
        userPreferencesRepository.userData
    ) { transactions, userData ->
        calculateUiState(transactions, userData)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialSummaryUiState(isLoading = true)
    )

    private fun calculateUiState(transactions: List<Transaction>, userData: UserData): FinancialSummaryUiState {
        val currentDate = LocalDate.now()
        val currentYear = currentDate.year
        val currentMonth = YearMonth.from(currentDate)

        if (transactions.isEmpty()) {
            return FinancialSummaryUiState(
                isLoading = false,
                currency = userData.currency
            )
        }

        // 1. Total Atual (All Time)
        val totalSummary = calculateSummary(transactions)

        // 2. Total Anual (Current Year)
        val yearlyTransactions = transactions.filter { it.date.year == currentYear }
        val yearlySummary = calculateSummary(yearlyTransactions)

        // 3. Mês Atual
        val monthlyTransactions = transactions.filter { YearMonth.from(it.date) == currentMonth }
        val monthlySummary = calculateSummary(monthlyTransactions)

        return FinancialSummaryUiState(
            isLoading = false,
            currency = userData.currency,
            currentYear = currentYear,
            totalSummary = totalSummary,
            yearlySummary = yearlySummary,
            monthlySummary = monthlySummary
        )
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
