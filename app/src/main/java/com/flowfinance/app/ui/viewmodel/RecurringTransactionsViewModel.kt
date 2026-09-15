package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.local.entity.RecurringTransaction
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.data.repository.RecurringTransactionRepository
import com.flowfinance.app.util.RecurrenceFrequency
import com.flowfinance.app.util.TransactionType
import com.flowfinance.app.worker.RecurringTransactionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RecurringTransactionsViewModel @Inject constructor(
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val recurringTransactions: StateFlow<List<RecurringTransaction>> =
        recurringTransactionRepository.getAllRecurringTransactions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createRecurringTransaction(
        description: String,
        amount: Double,
        type: TransactionType,
        categoryId: Int,
        frequency: RecurrenceFrequency,
        startDate: LocalDate,
        endDate: LocalDate?
    ) {
        viewModelScope.launch {
            recurringTransactionRepository.insertRecurringTransaction(
                RecurringTransaction(
                    description = description,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate
                )
            )
            triggerRecurringTransactionsCheck()
        }
    }

    fun updateRecurringTransaction(
        existing: RecurringTransaction,
        description: String,
        amount: Double,
        type: TransactionType,
        categoryId: Int,
        frequency: RecurrenceFrequency,
        startDate: LocalDate,
        endDate: LocalDate?
    ) {
        viewModelScope.launch {
            recurringTransactionRepository.updateRecurringTransaction(
                existing.copy(
                    description = description,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = endDate
                )
            )
            triggerRecurringTransactionsCheck()
        }
    }

    fun toggleActive(recurringTransaction: RecurringTransaction, isActive: Boolean) {
        viewModelScope.launch {
            recurringTransactionRepository.updateRecurringTransaction(
                recurringTransaction.copy(isActive = isActive)
            )
            if (isActive) triggerRecurringTransactionsCheck()
        }
    }

    fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction) {
        viewModelScope.launch {
            recurringTransactionRepository.deleteRecurringTransaction(recurringTransaction)
        }
    }

    private fun triggerRecurringTransactionsCheck() {
        val request = OneTimeWorkRequestBuilder<RecurringTransactionWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RecurringTransactionWorker.IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
