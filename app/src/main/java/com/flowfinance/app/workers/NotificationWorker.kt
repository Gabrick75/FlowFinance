package com.flowfinance.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flowfinance.app.R
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.data.repository.CategoryRepository
import com.flowfinance.app.util.NotificationHelper
import com.flowfinance.app.util.TransactionType
import com.flowfinance.app.util.formatCurrency
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_NOTIFICATION_TYPE) ?: return Result.failure()

        return try {
            when (type) {
                TYPE_WEEKLY_REMINDER -> sendWeeklyReminder()
                TYPE_BUDGET_CHECK -> checkBudgets()
                TYPE_TEST -> sendTestNotification()
                else -> Result.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun sendWeeklyReminder(): Result {
        showNotification(
            title = applicationContext.getString(R.string.notification_weekly_reminder_title),
            message = applicationContext.getString(R.string.notification_weekly_reminder_message),
            notificationId = ID_WEEKLY_REMINDER
        )
        return Result.success()
    }

    private fun sendTestNotification(): Result {
        showNotification(
            title = applicationContext.getString(R.string.notification_test_title),
            message = applicationContext.getString(R.string.notification_test_message),
            notificationId = ID_TEST_NOTIFICATION
        )
        return Result.success()
    }

    private suspend fun checkBudgets(): Result {
        val categories = categoryRepository.getAllCategories().first()
        val currentDate = LocalDate.now()
        val startDate = currentDate.withDayOfMonth(1)
        val endDate = currentDate.withDayOfMonth(currentDate.lengthOfMonth())

        // Get summaries for expense type in current month
        val summaries = transactionRepository.getCategorySummaryByTypeAndDateRange(
            TransactionType.EXPENSE,
            startDate,
            endDate
        ).first()

        // Map summaries for easier lookup
        val summaryMap = summaries.associate { it.category.id to it.totalAmount }

        categories.forEach { category ->
            val budget = category.budgetLimit
            if (budget != null && budget > 0) {
                val spent = summaryMap[category.id] ?: 0.0
                val percentage = (spent / budget) * 100

                // Check thresholds: 50, 70, 90, 100
                val threshold = when {
                    percentage >= 100 -> 100
                    percentage >= 90 -> 90
                    percentage >= 70 -> 70
                    percentage >= 50 -> 50
                    else -> null
                }

                if (threshold != null) {
                    val message = if (threshold == 100) {
                        applicationContext.getString(
                            R.string.notification_budget_alert_full,
                            formatCurrency(budget),
                            formatCurrency(spent)
                        )
                    } else {
                        applicationContext.getString(
                            R.string.notification_budget_alert_progress,
                            threshold,
                            formatCurrency(spent),
                            formatCurrency(budget)
                        )
                    }
                    showNotification(
                        title = applicationContext.getString(R.string.notification_budget_alert_title, category.name),
                        message = message,
                        notificationId = category.id
                    )
                }
            }
        }
        return Result.success()
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        NotificationHelper.show(applicationContext, title, message, notificationId)
    }

    companion object {
        const val KEY_NOTIFICATION_TYPE = "key_notification_type"
        const val TYPE_WEEKLY_REMINDER = "type_weekly_reminder"
        const val TYPE_BUDGET_CHECK = "type_budget_check"
        const val TYPE_TEST = "type_test"

        const val ID_WEEKLY_REMINDER = 1001
        const val ID_TEST_NOTIFICATION = 1002
    }
}
