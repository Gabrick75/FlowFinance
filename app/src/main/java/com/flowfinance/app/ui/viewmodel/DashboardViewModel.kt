package com.flowfinance.app.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.model.CategorySummary
import com.flowfinance.app.data.local.model.TransactionWithCategory
import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

@Immutable
data class DashboardUiState(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val currency: String = "BRL"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val categoryRepository: CategoryRepository // Injected CategoryRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<DashboardUiState> = combine(
        _currentMonth,
        transactionRepository.getAllTransactionsWithCategory(),
        userPreferencesRepository.userData
    ) { currentMonth, allTransactions, userData ->
        
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        
        val monthTransactions = allTransactions.filter { 
            !it.transaction.date.isBefore(startDate) && !it.transaction.date.isAfter(endDate) 
        }

        val income = monthTransactions
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
            
        val expense = monthTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }

        val totalIncome = allTransactions
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amount }
        val totalExpense = allTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amount }
        val balance = totalIncome - totalExpense

        val recent = allTransactions.take(5)

        DashboardUiState(
            totalBalance = balance,
            monthlyIncome = income,
            monthlyExpense = expense,
            recentTransactions = recent,
            currency = userData.currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chartData: StateFlow<List<CategorySummary>> = _currentMonth.flatMapLatest { month ->
        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()
        transactionRepository.getCategorySummaryByTypeAndDateRange(
            TransactionType.EXPENSE,
            startDate,
            endDate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
