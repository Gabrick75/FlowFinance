package com.flowfinance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val transactionsByDate: Map<LocalDate, List<Transaction>> = emptyMap()
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TransactionsUiState> = combine(
        _currentMonth,
        _searchQuery,
        transactionRepository.getAllTransactions()
    ) { currentMonth, query, allTransactions ->
        val startDate = currentMonth.atDay(1)
        val endDate = currentMonth.atEndOfMonth()
        
        var filteredTransactions = allTransactions.filter { 
            !it.date.isBefore(startDate) && !it.date.isAfter(endDate) 
        }

        if (query.isNotBlank()) {
            filteredTransactions = filteredTransactions.filter {
                it.description.contains(query, ignoreCase = true)
            }
        }

        val grouped = filteredTransactions
            .groupBy { it.date }
            .toSortedMap(compareByDescending { it }) // Recent dates first

        TransactionsUiState(
            currentMonth = currentMonth,
            searchQuery = query,
            transactionsByDate = grouped
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
}
