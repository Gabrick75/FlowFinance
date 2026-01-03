package com.flowfinance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val allCategories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseCategories: StateFlow<List<Category>> = allCategories.map {
        it.filter { category -> category.name != "Salário" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategories: StateFlow<List<Category>> = allCategories.map {
        it.filter { category ->
            category.name == "Salário" || category.name == "Investimentos" || !category.isDefault
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    fun saveTransaction(
        description: String,
        amount: Double,
        type: TransactionType,
        categoryId: Int,
        date: LocalDate = LocalDate.now()
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                description = description,
                amount = amount,
                type = type,
                categoryId = categoryId,
                date = date
            )
            transactionRepository.insertTransaction(transaction)
        }
    }
}
