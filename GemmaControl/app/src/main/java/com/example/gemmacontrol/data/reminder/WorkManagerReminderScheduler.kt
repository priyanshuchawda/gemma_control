package com.example.gemmacontrol.data.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class WorkManagerReminderScheduler(
    context: Context,
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val workManagerProvider: (Context) -> WorkManager = { WorkManager.getInstance(it) }
) : ReminderScheduler {
    private val appContext = context.applicationContext

    override suspend fun schedule(reminderId: String, remindAtEpochMillis: Long): String {
        val workName = ReminderWorkContract.workName(reminderId)
        val delayMillis = (remindAtEpochMillis - nowProvider()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(ReminderWorkContract.KEY_REMINDER_ID to reminderId)
            )
            .build()

        workManagerProvider(appContext).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            request
        )
        return workName
    }
}

object ReminderWorkContract {
    const val KEY_REMINDER_ID = "reminder_id"
    private const val UNIQUE_WORK_PREFIX = "whatsapp_reminder:"

    fun workName(reminderId: String): String = UNIQUE_WORK_PREFIX + reminderId
}
