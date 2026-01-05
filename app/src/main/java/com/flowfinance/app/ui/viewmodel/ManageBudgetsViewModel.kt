package com.flowfinance.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageBudgetsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    
    private val _deleteResult = MutableSharedFlow<DeleteResult>()
    val deleteResult = _deleteResult.asSharedFlow()

    fun updateBudget(category: Category, newBudget: Double?) {
        viewModelScope.launch {
            val updatedCategory = category.copy(budgetLimit = newBudget)
            categoryRepository.updateCategory(updatedCategory)
        }
    }

    fun createNewCategory(name: String, color: Int, icon: String?) {
        viewModelScope.launch {
            val newCategory = Category(
                name = name,
                color = color,
                icon = icon,
                budgetLimit = null // Starts with no budget
            )
            categoryRepository.insertCategory(newCategory)
        }
    }
    
    fun updateCategoryDetails(category: Category, newName: String, newColor: Int, newIcon: String?) {
        viewModelScope.launch {
            val updatedCategory = category.copy(
                name = newName,
                color = newColor,
                icon = newIcon
            )
            categoryRepository.updateCategory(updatedCategory)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            if (category.isDefault) {
                _deleteResult.emit(DeleteResult.Failure("Não é possível apagar categorias padrão."))
                return@launch
            }
            
            val transactionCount = transactionRepository.getTransactionCountForCategory(category.id)
            if (transactionCount > 0) {
                _deleteResult.emit(DeleteResult.Failure("Não é possível apagar categorias com transações associadas."))
                return@launch
            }
            
            categoryRepository.deleteCategory(category)
            _deleteResult.emit(DeleteResult.Success("Categoria apagada com sucesso!"))
        }
    }
}

sealed class DeleteResult {
    data class Success(val message: String) : DeleteResult()
    data class Failure(val message: String) : DeleteResult()
}
