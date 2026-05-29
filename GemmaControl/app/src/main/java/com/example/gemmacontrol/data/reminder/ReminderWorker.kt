package com.example.gemmacontrol.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.gemmacontrol.MainActivity
import com.example.gemmacontrol.data.crypto.AndroidKeystoreSensitiveTextCipher
import com.example.gemmacontrol.data.crypto.EncryptedPayload
import com.example.gemmacontrol.data.local.GemmaControlDatabase

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(ReminderWorkContract.KEY_REMINDER_ID)
            ?: return Result.failure(errorData("Missing reminder id."))
        val database = GemmaControlDatabase.getDatabase(applicationContext)
        val reminder = database.reminderDao().getById(reminderId)
            ?: return Result.failure(errorData("Reminder not found."))

        if (!hasNotificationPermission()) {
            return Result.failure(errorData("Notification permission is not granted."))
        }

        val note = decryptNote(reminder.encryptedReminderNote, reminder.reminderNoteIv)
        return try {
            ensureChannel()
            NotificationManagerCompat.from(applicationContext).notify(
                notificationId(reminder.id),
                buildNotification(note)
            )
            database.reminderDao().markDelivered(reminder.id, System.currentTimeMillis())
            Result.success()
        } catch (e: SecurityException) {
            Result.failure(errorData("Notification permission is not granted."))
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun decryptNote(ciphertext: ByteArray?, iv: ByteArray?): String? {
        if (ciphertext == null || iv == null) {
            return null
        }
        return try {
            AndroidKeystoreSensitiveTextCipher().decrypt(EncryptedPayload(ciphertext, iv))
        } catch (e: Exception) {
            null
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(note: String?): android.app.Notification {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("GemmaControl reminder")
            .setContentText(note ?: "Open GemmaControl to review the saved WhatsApp item.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun notificationId(reminderId: String): Int {
        return reminderId.hashCode() and Int.MAX_VALUE
    }

    private fun errorData(message: String) = workDataOf(KEY_ERROR to message)

    companion object {
        private const val CHANNEL_ID = "gemma_control_reminders"
        private const val CHANNEL_NAME = "WhatsApp reminders"
        const val KEY_ERROR = "reminder_error"
    }
}
