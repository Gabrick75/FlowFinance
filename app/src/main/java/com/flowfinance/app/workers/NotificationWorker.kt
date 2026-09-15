package com.flowfinance.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
            title = "Hora de atualizar suas finanças!",
            message = "Não se esqueça de lançar seus gastos da semana no FlowFinance.",
            notificationId = ID_WEEKLY_REMINDER
        )
        return Result.success()
    }
    
    private fun sendTestNotification(): Result {
        showNotification(
            title = "Teste de Notificação",
            message = "Se você está vendo isso, as notificações do FlowFinance estão funcionando corretamente!",
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
                if (percentage >= 100) {
                     showNotification(
                        title = "Alerta de Orçamento: ${category.name}",
                        message = "Você atingiu 100% da sua meta de ${formatCurrency(budget)}! Gasto: ${formatCurrency(spent)}",
                        notificationId = category.id
                    )
                } else if (percentage >= 90) {
                    showNotification(
                        title = "Alerta de Orçamento: ${category.name}",
                        message = "Você atingiu 90% da sua meta. Gasto: ${formatCurrency(spent)} de ${formatCurrency(budget)}",
                        notificationId = category.id
                    )
                } else if (percentage >= 70) {
                    showNotification(
                        title = "Alerta de Orçamento: ${category.name}",
                        message = "Você atingiu 70% da sua meta. Gasto: ${formatCurrency(spent)} de ${formatCurrency(budget)}",
                        notificationId = category.id
                    )
                } else if (percentage >= 50) {
                    showNotification(
                        title = "Alerta de Orçamento: ${category.name}",
                        message = "Você atingiu 50% da sua meta. Gasto: ${formatCurrency(spent)} de ${formatCurrency(budget)}",
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
