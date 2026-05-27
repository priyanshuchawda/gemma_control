package com.example.gemmacontrol.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WhatsAppNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "WhatsAppListener"
        private val _capturedNotifications = MutableStateFlow<List<ParsedNotification>>(emptyList())
        val capturedNotifications: StateFlow<List<ParsedNotification>> = _capturedNotifications.asStateFlow()

        fun clearList() {
            _capturedNotifications.value = emptyList()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected successfully")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName != "com.whatsapp" && sbn.packageName != "com.whatsapp.w4b") return

        Log.d(TAG, "WhatsApp notification posted from package: ${sbn.packageName}")
        val parsed = WhatsAppNotificationParser.parse(this, sbn) ?: return

        _capturedNotifications.update { current ->
            // Keep latest messages at the top, prevent duplicates based on dedupeHash
            val filtered = current.filter { it.dedupeHash != parsed.dedupeHash }
            listOf(parsed) + filtered
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        if (sbn.packageName != "com.whatsapp" && sbn.packageName != "com.whatsapp.w4b") return

        Log.d(TAG, "WhatsApp notification removed: ${sbn.key}")
        _capturedNotifications.update { current ->
            current.map {
                if (it.notificationKey == sbn.key) {
                    it.copy(isActive = false, removedAt = System.currentTimeMillis())
                } else {
                    it
                }
            }
        }
    }
}
