package com.flowfinance.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flowfinance.app.R
import com.flowfinance.app.data.local.entity.RecurringTransaction
import com.flowfinance.app.data.local.entity.Transaction
import com.flowfinance.app.data.repository.RecurringTransactionRepository
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.util.NotificationHelper
import com.flowfinance.app.util.occurrenceDate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            var generatedCount = 0

            recurringTransactionRepository.getActiveRecurringTransactions().forEach { rule ->
                generatedCount += generateDueOccurrences(rule, today)
            }

            if (generatedCount > 0) {
                NotificationHelper.show(
                    applicationContext,
                    applicationContext.getString(R.string.notification_recurring_title),
                    applicationContext.getString(R.string.notification_recurring_message, generatedCount),
                    ID_RECURRING_GENERATED
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("RecurringTransactionWorker", "Error generating recurring transactions", e)
            Result.failure()
        }
    }

    private suspend fun generateDueOccurrences(rule: RecurringTransaction, today: LocalDate): Int {
        var occurrencesGenerated = rule.occurrencesGenerated
        var nextDate = rule.frequency.occurrenceDate(rule.startDate, occurrencesGenerated.toLong())
        var createdCount = 0

        while (!nextDate.isAfter(today) && (rule.endDate == null || !nextDate.isAfter(rule.endDate))) {
            transactionRepository.insertTransaction(
                Transaction(
                    description = rule.description,
                    amount = rule.amount,
                    date = nextDate,
                    type = rule.type,
                    categoryId = rule.categoryId
                )
            )
            createdCount++
            occurrencesGenerated++
            nextDate = rule.frequency.occurrenceDate(rule.startDate, occurrencesGenerated.toLong())
        }

        if (createdCount > 0) {
            val isPastEndDate = rule.endDate != null && nextDate.isAfter(rule.endDate)
            recurringTransactionRepository.updateRecurringTransaction(
                rule.copy(
                    occurrencesGenerated = occurrencesGenerated,
                    isActive = rule.isActive && !isPastEndDate
                )
            )
        }

        return createdCount
    }

    companion object {
        const val ID_RECURRING_GENERATED = 1003
        const val IMMEDIATE_WORK_NAME = "RecurringTransactionsImmediateCheck"
    }
}
