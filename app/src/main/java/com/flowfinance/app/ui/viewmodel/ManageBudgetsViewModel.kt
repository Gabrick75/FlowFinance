package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.workers.NotificationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeleteResult {
    data class Success(val message: String) : DeleteResult()
    data class Failure(val message: String) : DeleteResult()
}

@HiltViewModel
class ManageBudgetsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deleteResult = MutableSharedFlow<DeleteResult>()
    val deleteResult = _deleteResult.asSharedFlow()

    fun updateBudget(category: Category, newBudget: Double?) {
        viewModelScope.launch {
            val updatedCategory = category.copy(budgetLimit = newBudget)
            categoryRepository.updateCategory(updatedCategory)

            // Trigger budget check as budget limit changed
            if (newBudget != null && newBudget > 0) {
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

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                if (category.isDefault) {
                     _deleteResult.emit(DeleteResult.Failure("Categorias padrão não podem ser excluídas."))
                     return@launch
                }
                categoryRepository.deleteCategory(category)
                _deleteResult.emit(DeleteResult.Success("Categoria excluída com sucesso."))
            } catch (e: Exception) {
                // Ideally catch specific constraint exception if transactions exist
                _deleteResult.emit(DeleteResult.Failure("Erro ao excluir. Verifique se existem transações vinculadas."))
            }
        }
    }

    fun createNewCategory(name: String, color: Int, iconName: String) {
        viewModelScope.launch {
            val newCategory = Category(
                name = name,
                color = color,
                icon = iconName,
                isDefault = false
            )
            categoryRepository.insertCategory(newCategory)
        }
    }

    fun updateCategoryDetails(category: Category, name: String, color: Int, iconName: String) {
        viewModelScope.launch {
            val updatedCategory = category.copy(
                name = name,
                color = color,
                icon = iconName
            )
            categoryRepository.updateCategory(updatedCategory)
        }
    }
}
