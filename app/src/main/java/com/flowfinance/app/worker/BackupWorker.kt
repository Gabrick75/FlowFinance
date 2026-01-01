package com.flowfinance.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flowfinance.app.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val transactions = transactionRepository.getAllTransactions().first()
            
            val fileName = "backup_transactions_${System.currentTimeMillis()}.csv"
            val file = File(applicationContext.filesDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.append("Id,Description,Amount,Date,Type,CategoryId\n")
                transactions.forEach {
                    writer.append("${it.id},${it.description},${it.amount},${it.date},${it.type},${it.categoryId}\n")
                }
            }
            
            Log.d("BackupWorker", "Backup created at ${file.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Error creating backup", e)
            Result.failure()
        }
    }
}
