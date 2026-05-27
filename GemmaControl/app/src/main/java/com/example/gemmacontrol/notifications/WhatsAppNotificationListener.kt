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
        private const val MAX_HISTORY_LIMIT = 100

        private val _capturedNotifications = MutableStateFlow<List<ParsedWhatsAppNotificationEvent>>(emptyList())
        val capturedNotifications: StateFlow<List<ParsedWhatsAppNotificationEvent>> = _capturedNotifications.asStateFlow()

        // Set to track keys that are currently active in the status bar
        private val activeKeys = mutableSetOf<String>()

        fun clearList() {
            _capturedNotifications.value = emptyList()
            synchronized(activeKeys) {
                activeKeys.clear()
            }
        }

        /**
         * For unit testing simulation of POSTED event state flow.
         */
        fun postNotificationForTest(parsed: ParsedWhatsAppNotificationEvent) {
            _capturedNotifications.update { current ->
                val newEvent = parsed.copy(observedAt = System.currentTimeMillis())
                synchronized(activeKeys) {
                    activeKeys.add(parsed.notificationKey)
                }
                val updatedPrevious = current.map {
                    if (it.notificationKey == parsed.notificationKey) {
                        it.copy(isCurrentlyActive = false)
                    } else {
                        it
                    }
                }
                (listOf(newEvent) + updatedPrevious).take(MAX_HISTORY_LIMIT)
            }
        }

        /**
         * For unit testing simulation of REMOVED event state flow.
         */
        fun removeNotificationForTest(key: String) {
            synchronized(activeKeys) {
                if (!activeKeys.contains(key)) return
                activeKeys.remove(key)
            }
            _capturedNotifications.update { current ->
                val lastEvent = current.firstOrNull { it.notificationKey == key }
                val removedEvent = ParsedWhatsAppNotificationEvent(
                    eventType = NotificationEventType.REMOVED,
                    notificationKey = key,
                    packageName = lastEvent?.packageName ?: "com.whatsapp",
                    observedAt = System.currentTimeMillis(),
                    notificationPostedAt = lastEvent?.notificationPostedAt,
                    conversationTitle = lastEvent?.conversationTitle,
                    conversationType = lastEvent?.conversationType ?: ConversationType.UNKNOWN,
                    messages = lastEvent?.messages ?: emptyList(),
                    currentMessageCount = lastEvent?.currentMessageCount ?: 0,
                    historicMessageCount = lastEvent?.historicMessageCount ?: 0,
                    hasReplyActionAtCaptureTime = lastEvent?.hasReplyActionAtCaptureTime ?: false,
                    parseSource = lastEvent?.parseSource ?: NotificationParseSource.UNAVAILABLE,
                    isContentUnavailable = lastEvent?.isContentUnavailable ?: true,
                    dedupeCandidate = lastEvent?.dedupeCandidate,
                    isCurrentlyActive = false
                )
                
                // Mark previous items of this key as inactive
                val updatedPrevious = current.map {
                    if (it.notificationKey == key) {
                        it.copy(isCurrentlyActive = false)
                    } else {
                        it
                    }
                }
                (listOf(removedEvent) + updatedPrevious).take(MAX_HISTORY_LIMIT)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected successfully")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val packageName = sbn.packageName ?: return
        if (!WhatsAppNotificationParser.isPackageSupported(packageName)) return

        val key = sbn.key ?: ""
        val keySuffix = if (key.length > 8) key.takeLast(8) else key
        
        val isUpdate = synchronized(activeKeys) {
            val contains = activeKeys.contains(key)
            activeKeys.add(key)
            contains
        }

        val eventType = if (isUpdate) NotificationEventType.UPDATED else NotificationEventType.POSTED
        Log.d(TAG, "Supported package observed: $packageName, Event Type: $eventType, Key Suffix: $keySuffix")

        val parsed = WhatsAppNotificationParser.parse(this, sbn, eventType, isCurrentlyActive = true)
        if (parsed == null) {
            Log.e(TAG, "Failed to parse notification. Key Suffix: $keySuffix")
            return
        }

        Log.d(TAG, "Parse success! Source: ${parsed.parseSource}, Messages: ${parsed.currentMessageCount}")

        _capturedNotifications.update { current ->
            // Mark previous items of this key as inactive
            val updatedPrevious = current.map {
                if (it.notificationKey == key) {
                    it.copy(isCurrentlyActive = false)
                } else {
                    it
                }
            }
            (listOf(parsed) + updatedPrevious).take(MAX_HISTORY_LIMIT)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        val packageName = sbn.packageName ?: return
        if (!WhatsAppNotificationParser.isPackageSupported(packageName)) return

        val key = sbn.key ?: ""
        val keySuffix = if (key.length > 8) key.takeLast(8) else key

        val existed = synchronized(activeKeys) {
            activeKeys.remove(key)
        }
        if (!existed) return // Ignore if not currently active

        Log.d(TAG, "Notification removed: $packageName, Event Type: REMOVED, Key Suffix: $keySuffix")

        _capturedNotifications.update { current ->
            val lastEvent = current.firstOrNull { it.notificationKey == key }
            val removedEvent = ParsedWhatsAppNotificationEvent(
                eventType = NotificationEventType.REMOVED,
                notificationKey = key,
                packageName = packageName,
                observedAt = System.currentTimeMillis(),
                notificationPostedAt = lastEvent?.notificationPostedAt,
                conversationTitle = lastEvent?.conversationTitle,
                conversationType = lastEvent?.conversationType ?: ConversationType.UNKNOWN,
                messages = lastEvent?.messages ?: emptyList(),
                currentMessageCount = lastEvent?.currentMessageCount ?: 0,
                historicMessageCount = lastEvent?.historicMessageCount ?: 0,
                hasReplyActionAtCaptureTime = lastEvent?.hasReplyActionAtCaptureTime ?: false,
                parseSource = lastEvent?.parseSource ?: NotificationParseSource.UNAVAILABLE,
                isContentUnavailable = lastEvent?.isContentUnavailable ?: true,
                dedupeCandidate = lastEvent?.dedupeCandidate,
                isCurrentlyActive = false
            )

            // Mark previous items of this key as inactive
            val updatedPrevious = current.map {
                if (it.notificationKey == key) {
                    it.copy(isCurrentlyActive = false)
                } else {
                    it
                }
            }
            (listOf(removedEvent) + updatedPrevious).take(MAX_HISTORY_LIMIT)
        }
    }
}
