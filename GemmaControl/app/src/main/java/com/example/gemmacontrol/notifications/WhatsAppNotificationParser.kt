package com.example.gemmacontrol.notifications

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import java.security.MessageDigest

data class ParsedNotification(
    val notificationKey: String,
    val senderName: String,
    val conversationTitle: String,
    val messageText: String,
    val postedAt: Long,
    val isGroup: Boolean,
    val hasReplyAction: Boolean,
    val isRedacted: Boolean,
    val dedupeHash: String,
    val isActive: Boolean = true,
    val removedAt: Long? = null
)

object WhatsAppNotificationParser {

    fun parse(context: Context, sbn: StatusBarNotification): ParsedNotification? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        val key = sbn.key ?: ""
        val postedAt = sbn.postTime
        val hasReply = notification.actions?.any { it.remoteInputs != null } == true

        // 1. Try to extract from MessagingStyle
        val recoverBuilder = try {
            Notification.Builder.recoverBuilder(context, notification)
        } catch (e: Exception) {
            null
        }

        val style = recoverBuilder?.style
        if (style is Notification.MessagingStyle) {
            val isGroup = style.isGroupConversation
            val conversationTitle = style.conversationTitle?.toString() ?: ""
            val messages = style.messages

            if (messages.isNotEmpty()) {
                val latestMessage = messages.last()
                val messageText = latestMessage.text?.toString() ?: ""
                val senderName = latestMessage.sender?.toString() ?: ""
                val isRedacted = messageText.isEmpty()

                val title = if (isGroup && conversationTitle.isNotEmpty()) {
                    conversationTitle
                } else {
                    senderName
                }

                val hash = generateDedupeHash(title, senderName, messageText, latestMessage.timestamp)

                return ParsedNotification(
                    notificationKey = key,
                    senderName = senderName,
                    conversationTitle = title,
                    messageText = messageText,
                    postedAt = latestMessage.timestamp,
                    isGroup = isGroup,
                    hasReplyAction = hasReply,
                    isRedacted = isRedacted,
                    dedupeHash = hash
                )
            }
        }

        // 2. Fallback to standard extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val isRedacted = text.isEmpty()

        val hash = generateDedupeHash(title, title, text, postedAt)

        return ParsedNotification(
            notificationKey = key,
            senderName = title,
            conversationTitle = title,
            messageText = text,
            postedAt = postedAt,
            isGroup = isGroup,
            hasReplyAction = hasReply,
            isRedacted = isRedacted,
            dedupeHash = hash
        )
    }

    private fun generateDedupeHash(conversation: String, sender: String, text: String, timestamp: Long): String {
        val rawString = "$conversation|$sender|$text|$timestamp"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawString.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            rawString.hashCode().toString()
        }
    }
}
