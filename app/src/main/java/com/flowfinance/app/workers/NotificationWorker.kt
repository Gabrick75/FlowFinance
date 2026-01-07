package com.flowfinance.app.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flowfinance.app.MainActivity
import com.flowfinance.app.R
import com.flowfinance.app.data.repository.TransactionRepository
import com.flowfinance.app.data.repository.CategoryRepository
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Cannot post notification without permission
                return
            }
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes e Alertas",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val systemNotificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Changed to use mipmap which exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val KEY_NOTIFICATION_TYPE = "key_notification_type"
        const val TYPE_WEEKLY_REMINDER = "type_weekly_reminder"
        const val TYPE_BUDGET_CHECK = "type_budget_check"
        const val TYPE_TEST = "type_test"
        
        const val CHANNEL_ID = "flow_finance_channel"
        const val ID_WEEKLY_REMINDER = 1001
        const val ID_TEST_NOTIFICATION = 1002
    }
}
