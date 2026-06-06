package com.example.gemmacontrol.notifications

import java.util.Locale

object RecentOutgoingReplyEchoSuppressor {
    private const val ReplyEchoWindowMillis = 2 * 60 * 1000L

    private val entries = mutableMapOf<String, OutgoingReplyEcho>()

    fun register(
        notificationKey: String,
        replyText: String,
        conversationTitle: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val normalizedText = replyText.normalizeForEchoMatch()
        val normalizedConversation = conversationTitle
            ?.normalizeForEchoMatch()
            ?.takeIf { it.isNotBlank() }
        if (notificationKey.isBlank() || normalizedText.isBlank()) {
            return
        }
        synchronized(entries) {
            entries[notificationKey] = OutgoingReplyEcho(
                normalizedText = normalizedText,
                normalizedConversation = normalizedConversation,
                expiresAt = nowMillis + ReplyEchoWindowMillis
            )
            pruneExpiredLocked(nowMillis)
        }
    }

    fun shouldSuppress(
        event: ParsedWhatsAppNotificationEvent,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val latestText = event.messages.lastOrNull()
            ?.messageText
            ?.normalizeForEchoMatch()
            .orEmpty()
        if (latestText.isBlank()) {
            return false
        }

        synchronized(entries) {
            pruneExpiredLocked(nowMillis)
            val eventConversation = event.conversationTitle
                ?.normalizeForEchoMatch()
                ?.takeIf { it.isNotBlank() }
            val exactKeyEcho = entries[event.notificationKey]
            if (exactKeyEcho?.normalizedText == latestText) {
                entries.remove(event.notificationKey)
                return true
            }

            val refreshedKeyEcho = entries.entries.firstOrNull { (_, echo) ->
                echo.normalizedConversation != null &&
                    echo.normalizedConversation == eventConversation &&
                    echo.normalizedText == latestText
            } ?: return false

            entries.remove(refreshedKeyEcho.key)
            return true
        }
    }

    fun clear() {
        synchronized(entries) {
            entries.clear()
        }
    }

    private fun pruneExpiredLocked(nowMillis: Long) {
        entries.entries.removeAll { (_, echo) -> echo.expiresAt < nowMillis }
    }

    private fun String.normalizeForEchoMatch(): String {
        return trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    private data class OutgoingReplyEcho(
        val normalizedText: String,
        val normalizedConversation: String?,
        val expiresAt: Long
    )
}
