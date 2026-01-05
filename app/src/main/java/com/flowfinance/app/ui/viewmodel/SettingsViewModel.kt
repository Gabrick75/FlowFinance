package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.preferences.UserData
import com.flowfinance.app.data.preferences.UserPreferencesRepository
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val userData: StateFlow<UserData> = userPreferencesRepository.userData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserData("Usuário", "BRL", false)
        )

    fun updateTheme(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkTheme(isDark)
        }
    }
    
    fun updateUserName(name: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserName(name)
        }
    }
    
    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCurrency(currency)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            // First delete all transactions
            transactionRepository.deleteAllTransactions()
            // Then delete all custom categories
            categoryRepository.deleteAllCustomCategories()
        }
    }

    fun exportDataToCsv(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val transactionsWithCategory = transactionRepository.getAllTransactionsWithCategory().first()
                val userData = userPreferencesRepository.userData.first()
                
                val fileName = "flowfinance_export_${System.currentTimeMillis()}.csv"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                FileWriter(file).use { writer ->
                    // Custom Header as requested: AppName, UserName, Currency
                    writer.append("FlowFinance,${userData.userName},${userData.currency}\n")
                    writer.append("\n") // Blank line separator
                    
                    // Transaction Data
                    writer.append("Id,Description,Amount,Date,Type,CategoryName\n")
                    transactionsWithCategory.forEach { (transaction, category) ->
                        writer.append("${transaction.id},${transaction.description},${transaction.amount},${transaction.date},${transaction.type},${category.name}\n")
                    }
                }
                
                onResult(file.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }
}
