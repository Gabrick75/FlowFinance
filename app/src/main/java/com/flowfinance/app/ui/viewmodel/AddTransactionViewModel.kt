package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.local.entity.RecurringTransaction
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.data.repository.RecurringTransactionRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.RecurrenceFrequency
import com.flowfinance.app.util.TransactionType
import com.flowfinance.app.worker.RecurringTransactionWorker
import com.flowfinance.app.workers.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val allCategories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseCategories: StateFlow<List<Category>> = allCategories.map {
        it.filter { category -> category.name != "Salário" && category.name != "Rendimentos" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategories: StateFlow<List<Category>> = allCategories.map {
        it.filter { category ->
            category.name == "Salário" || category.name == "Investimentos" || category.name == "Rendimentos" || !category.isDefault
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    fun saveTransaction(
        description: String,
        amount: Double,
        type: TransactionType,
        categoryId: Int,
        date: LocalDate = LocalDate.now(),
        recurrence: RecurrenceFrequency? = null,
        recurrenceEndDate: LocalDate? = null
    ) {
        viewModelScope.launch {
            if (recurrence != null) {
                recurringTransactionRepository.insertRecurringTransaction(
                    RecurringTransaction(
                        description = description,
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        frequency = recurrence,
                        startDate = date,
                        endDate = recurrenceEndDate
                    )
                )
                triggerRecurringTransactionsCheck()
                return@launch
            }

            val transaction = Transaction(
                description = description,
                amount = amount,
                type = type,
                categoryId = categoryId,
                date = date
            )
            transactionRepository.insertTransaction(transaction)

            // Trigger budget check for expenses
            if (type == TransactionType.EXPENSE) {
                val budgetCheckRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInputData(
                        androidx.work.Data.Builder()
                            .putString(NotificationWorker.KEY_NOTIFICATION_TYPE, NotificationWorker.TYPE_BUDGET_CHECK)
                            .build()
                    )
                    .build()
                WorkManager.getInstance(context).enqueue(budgetCheckRequest)
            }
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
