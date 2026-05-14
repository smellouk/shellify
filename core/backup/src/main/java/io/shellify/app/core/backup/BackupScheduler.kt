package io.shellify.app.core.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {

    private const val WORK_NAME = "shellify_scheduled_backup"

    fun schedule(context: Context, schedule: BackupSchedule) {
        val wm = WorkManager.getInstance(context)
        when (schedule) {
            BackupSchedule.NONE -> wm.cancelUniqueWork(WORK_NAME)
            BackupSchedule.WEEKLY -> enqueue(wm, 7, TimeUnit.DAYS)
            BackupSchedule.MONTHLY -> enqueue(wm, 30, TimeUnit.DAYS)
        }
    }

    private fun enqueue(wm: WorkManager, interval: Long, unit: TimeUnit) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(interval, unit)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
