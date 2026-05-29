package com.example.gemmacontrol.data.reminder

interface ReminderScheduler {
    suspend fun schedule(reminderId: String, remindAtEpochMillis: Long): String
}
