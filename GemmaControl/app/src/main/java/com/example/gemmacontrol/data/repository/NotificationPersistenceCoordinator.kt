package com.example.gemmacontrol.data.repository

import com.example.gemmacontrol.data.preferences.CapturePreferencesRepository
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import kotlinx.coroutines.flow.first

class NotificationPersistenceCoordinator(
    private val storedInboxRepository: StoredInboxRepository,
    private val preferencesRepository: CapturePreferencesRepository,
    private val activeNotificationReferenceDao: com.example.gemmacontrol.data.local.dao.ActiveNotificationReferenceDao
) {
    /**
     * Processes an incoming parsed notification event, applying consent checks,
     * deduplication rules, and the dual-notification ingestion policy.
     */
    suspend fun handleNotificationEvent(event: ParsedWhatsAppNotificationEvent) {
        val captureEnabled = preferencesRepository.captureEnabledFlow.first()
        val storageEnabled = preferencesRepository.storageEnabledFlow.first()
        val storageEnabledAt = preferencesRepository.storageEnabledAtFlow.first()

        // If capture is disabled, do not process
        if (!captureEnabled) return

        // If event is REMOVED, update active references
        if (event.eventType == NotificationEventType.REMOVED) {
            storedInboxRepository.markNotificationRemoved(event.notificationKey)
            return
        }

        // Storage toggle must be ON for database writes
        if (!storageEnabled) return

        if (event.parseSource == NotificationParseSource.UNAVAILABLE || event.isContentUnavailable) {
            return
        }

        // Consent Boundary: Do not persist message events whose timestamp predates the storage consent enabled time.
        // This blocks stacking/importing history that was visible prior to opt-in storage.
        val latestMessage = event.messages.lastOrNull()
        val messageTime = latestMessage?.timestamp ?: event.notificationPostedAt ?: System.currentTimeMillis()
        if (messageTime < storageEnabledAt) {
            return
        }

        if (event.parseSource == NotificationParseSource.MESSAGING_STYLE) {
            storedInboxRepository.persistCanonicalEvent(event)
        }
    }
}
