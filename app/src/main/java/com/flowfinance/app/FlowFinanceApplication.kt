package com.flowfinance.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.flowfinance.app.worker.BackupWorker
import com.flowfinance.app.worker.RecurringTransactionWorker
import com.flowfinance.app.workers.NotificationWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FlowFinanceApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupBackupWorker()
        setupWeeklyReminder()
        setupRecurringTransactionsWorker()
    }

    private fun setupBackupWorker() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklyBackup",
            ExistingPeriodicWorkPolicy.KEEP,
            backupRequest
        )
    }

    private fun setupRecurringTransactionsWorker() {
        val periodicRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecurringTransactionsCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        // Run once immediately so occurrences due since the last launch show up right away,
        // instead of waiting for the periodic worker's next cycle.
        val immediateRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            RecurringTransactionWorker.IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }

    private fun setupWeeklyReminder() {
        val now = Calendar.getInstance()
        val nextSunday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 9) // 9:00 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (nextSunday.before(now)) {
            nextSunday.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val initialDelay = nextSunday.timeInMillis - now.timeInMillis

        val reminderRequest = PeriodicWorkRequestBuilder<NotificationWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(
                androidx.work.Data.Builder()
                    .putString(NotificationWorker.KEY_NOTIFICATION_TYPE, NotificationWorker.TYPE_WEEKLY_REMINDER)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklyReminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }
}
