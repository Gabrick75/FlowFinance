package com.flowfinance.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowfinance.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    fun clearAllData() {
        viewModelScope.launch {
            transactionRepository.deleteAllTransactions()
        }
    }

    fun exportDataToCsv(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                
                // Create a file in external downloads or documents would require permissions
                // For simplicity and to stick to modern scoped storage, we'll save to app cache and 
                // the user would need a way to share/view it (via Intent which UI can handle).
                // Or we can just return the file path and UI launches a share intent.
                
                val fileName = "flowfinance_export_${System.currentTimeMillis()}.csv"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                FileWriter(file).use { writer ->
                    writer.append("Id,Description,Amount,Date,Type,CategoryId\n")
                    transactions.forEach {
                        writer.append("${it.id},${it.description},${it.amount},${it.date},${it.type},${it.categoryId}\n")
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
