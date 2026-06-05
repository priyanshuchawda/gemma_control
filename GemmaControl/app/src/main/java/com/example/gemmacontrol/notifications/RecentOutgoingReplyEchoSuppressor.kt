package com.example.gemmacontrol.notifications

import java.util.Locale

object RecentOutgoingReplyEchoSuppressor {
    private const val ReplyEchoWindowMillis = 2 * 60 * 1000L

    private val entries = mutableMapOf<String, OutgoingReplyEcho>()

    fun register(
        notificationKey: String,
        replyText: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val normalizedText = replyText.normalizeForEchoMatch()
        if (notificationKey.isBlank() || normalizedText.isBlank()) {
            return
        }
        synchronized(entries) {
            entries[notificationKey] = OutgoingReplyEcho(
                normalizedText = normalizedText,
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
            val echo = entries[event.notificationKey] ?: return false
            val matches = echo.normalizedText == latestText
            if (matches) {
                entries.remove(event.notificationKey)
            }
            return matches
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
        val expiresAt: Long
    )
}
