package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.local.model.TransactionWithCategory
import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class TransactionsUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val searchQuery: String = "",
    val transactionsByDate: Map<LocalDate, List<TransactionWithCategory>> = emptyMap(),
    val currency: String = "BRL"
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TransactionsUiState> = combine(
        _currentMonth,
        _searchQuery,
        transactionRepository.getAllTransactionsWithCategory(),
        userPreferencesRepository.userData
    ) { currentMonth, query, allTransactions, userData ->
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        
        var filteredTransactions = allTransactions.filter { 
            !it.transaction.date.isBefore(startDate) && !it.transaction.date.isAfter(endDate) 
        }

        if (query.isNotBlank()) {
            filteredTransactions = filteredTransactions.filter {
                it.transaction.description.contains(query, ignoreCase = true) ||
                it.category.name.contains(query, ignoreCase = true)
            }
        }

        val grouped = filteredTransactions
            .groupBy { it.transaction.date }
            .toSortedMap(compareByDescending { it }) // Recent dates first

        TransactionsUiState(
            currentMonth = currentMonth,
            searchQuery = query,
            transactionsByDate = grouped,
            currency = userData.currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun nextMonth() {
        _currentMonth.update { it.plusMonths(1) }
    }

    fun previousMonth() {
        _currentMonth.update { it.minusMonths(1) }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun exportMonthlyReport(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val state = uiState.value
                val file = PdfReportGenerator.generate(
                    context = context,
                    yearMonth = state.currentMonth,
                    transactionsByDate = state.transactionsByDate,
                    currency = state.currency
                )
                onResult(file.absolutePath)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
}
