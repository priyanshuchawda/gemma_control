package com.example.gemmacontrol.notifications

import android.app.Notification
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi
import java.nio.charset.StandardCharsets
import java.util.Locale

object WhatsAppNotificationParser {

    val SUPPORTED_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

    /**
     * Determines whether a package name belongs to a supported WhatsApp variant.
     */
    fun isPackageSupported(packageName: String?): Boolean {
        return packageName != null && SUPPORTED_PACKAGES.contains(packageName)
    }

    fun classifyMessageContent(messageText: String?): WhatsAppContentKind {
        val normalized = normalizeNotificationText(messageText)
        if (normalized.isBlank()) {
            return WhatsAppContentKind.HIDDEN
        }

        return when {
            isHiddenPlaceholder(normalized) -> WhatsAppContentKind.HIDDEN
            isUnsupportedPlaceholder(normalized) -> WhatsAppContentKind.UNKNOWN
            isMissedCallPlaceholder(normalized) -> WhatsAppContentKind.MISSED_CALL
            isSystemNotificationText(normalized) -> WhatsAppContentKind.SYSTEM
            isPhotoPlaceholder(normalized) -> WhatsAppContentKind.PHOTO
            isVideoPlaceholder(normalized) -> WhatsAppContentKind.VIDEO
            isStickerPlaceholder(normalized) -> WhatsAppContentKind.STICKER
            isAudioPlaceholder(normalized) -> WhatsAppContentKind.AUDIO
            isDocumentPlaceholder(normalized) -> WhatsAppContentKind.DOCUMENT
            else -> WhatsAppContentKind.TEXT
        }
    }

    /**
     * Parses an incoming notification from WhatsApp into a structured local in-memory model.
     * Note: Final deduplication correctness is still considered unverified until real stacked/reposted
     * WhatsApp notifications are observed and validated directly on the physical phone.
     */
    fun parse(
        context: Context,
        sbn: StatusBarNotification,
        eventType: NotificationEventType,
        isCurrentlyActive: Boolean
    ): ParsedWhatsAppNotificationEvent? {
        val packageName = sbn.packageName ?: return null
        if (!isPackageSupported(packageName)) return null

        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        val key = sbn.key ?: ""
        val observedAt = System.currentTimeMillis()
        val notificationPostedAt = sbn.postTime
        val hasReply = notification.actions?.any { it.remoteInputs != null } == true

        // 1. Attempt to parse MessagingStyle if available
        val recoverBuilder = try {
            Notification.Builder.recoverBuilder(context, notification)
        } catch (e: Exception) {
            null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            parseMessagingStyle(
                builder = recoverBuilder,
                packageName = packageName,
                key = key,
                observedAt = observedAt,
                notificationPostedAt = notificationPostedAt,
                eventType = eventType,
                hasReply = hasReply,
                isCurrentlyActive = isCurrentlyActive
            )?.let { return it }
        }

        // 2. Fallback to standard extras parsing
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        val contentKind = classifyMessageContent(text)
        val isContentUnavailable = (title.isEmpty() && text.isEmpty()) ||
            contentKind == WhatsAppContentKind.HIDDEN

        // Fallbacks default to UNKNOWN and EXTRAS_FALLBACK
        val conversationType = ConversationType.UNKNOWN

        val msgPreviews = if (isContentUnavailable) {
            emptyList()
        } else {
            listOf(
                ParsedMessagePreview(
                    senderName = title.ifEmpty { null },
                    messageText = text.ifEmpty { null },
                    timestamp = notificationPostedAt,
                    contentKind = contentKind
                )
            )
        }

        val dedupe = generateDedupeCandidate(
            packageName = packageName,
            notificationKey = key,
            timestamp = notificationPostedAt,
            conversationTitle = title,
            senderName = title,
            messageText = text
        )

        return ParsedWhatsAppNotificationEvent(
            eventType = eventType,
            notificationKey = key,
            packageName = packageName,
            observedAt = observedAt,
            notificationPostedAt = notificationPostedAt,
            conversationTitle = title.ifEmpty { null },
            conversationType = conversationType,
            messages = msgPreviews,
            currentMessageCount = msgPreviews.size,
            historicMessageCount = 0,
            hasReplyActionAtCaptureTime = hasReply,
            parseSource = if (isContentUnavailable) NotificationParseSource.UNAVAILABLE else NotificationParseSource.EXTRAS_FALLBACK,
            isContentUnavailable = isContentUnavailable,
            dedupeCandidate = dedupe,
            isCurrentlyActive = isCurrentlyActive
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun parseMessagingStyle(
        builder: Notification.Builder?,
        packageName: String,
        key: String,
        observedAt: Long,
        notificationPostedAt: Long,
        eventType: NotificationEventType,
        hasReply: Boolean,
        isCurrentlyActive: Boolean
    ): ParsedWhatsAppNotificationEvent? {
        val style = builder?.style
        if (style !is Notification.MessagingStyle) return null

        val isGroup = style.isGroupConversation
        val conversationTitle = style.conversationTitle?.toString() ?: ""
        val messages = style.messages ?: emptyList()
        val historicMessages = style.historicMessages ?: emptyList()

        val currentMsgPreviews = messages.map { msg ->
            val text = msg.text?.toString()
            ParsedMessagePreview(
                senderName = msg.senderDisplayName(),
                messageText = text,
                timestamp = msg.timestamp,
                contentKind = classifyMessageContent(text)
            )
        }

        val historicMsgPreviews = historicMessages.map { msg ->
            val text = msg.text?.toString()
            ParsedMessagePreview(
                senderName = msg.senderDisplayName(),
                messageText = text,
                timestamp = msg.timestamp,
                contentKind = classifyMessageContent(text)
            )
        }

        val allMessages = currentMsgPreviews + historicMsgPreviews
        val latestMessage = currentMsgPreviews.lastOrNull()
        val latestSender = latestMessage?.senderName ?: ""
        val latestText = latestMessage?.messageText ?: ""
        val latestContentKind = latestMessage?.contentKind ?: WhatsAppContentKind.HIDDEN

        val conversationType = if (isGroup) {
            ConversationType.GROUP
        } else if (conversationTitle.isEmpty() || conversationTitle == latestSender) {
            ConversationType.DIRECT
        } else {
            ConversationType.UNKNOWN
        }

        val isContentUnavailable = currentMsgPreviews.isEmpty() ||
            latestContentKind == WhatsAppContentKind.HIDDEN
        val timestampToUse = latestMessage?.timestamp ?: notificationPostedAt

        val dedupe = generateDedupeCandidate(
            packageName = packageName,
            notificationKey = key,
            timestamp = timestampToUse,
            conversationTitle = conversationTitle,
            senderName = latestSender,
            messageText = latestText
        )

        return ParsedWhatsAppNotificationEvent(
            eventType = eventType,
            notificationKey = key,
            packageName = packageName,
            observedAt = observedAt,
            notificationPostedAt = notificationPostedAt,
            conversationTitle = conversationTitle.ifEmpty { latestSender.ifEmpty { null } },
            conversationType = conversationType,
            messages = allMessages,
            currentMessageCount = currentMsgPreviews.size,
            historicMessageCount = historicMsgPreviews.size,
            hasReplyActionAtCaptureTime = hasReply,
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            isContentUnavailable = isContentUnavailable,
            dedupeCandidate = dedupe,
            isCurrentlyActive = isCurrentlyActive
        )
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.P)
    private fun Notification.MessagingStyle.Message.senderDisplayName(): String? {
        return senderPerson?.name?.toString() ?: sender?.toString()
    }

    /**
     * Pure Kotlin deterministic dedupe candidate SHA-256 calculation.
     */
    internal fun generateDedupeCandidate(
        packageName: String,
        notificationKey: String,
        timestamp: Long,
        conversationTitle: String,
        senderName: String,
        messageText: String
    ): String {
        val rawString = "$packageName|$notificationKey|$timestamp|$conversationTitle|$senderName|$messageText"
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(rawString.toByteArray(StandardCharsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            rawString.hashCode().toString()
        }
    }

    private fun normalizeNotificationText(messageText: String?): String {
        return messageText
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace(Regex("[^\\p{L}\\p{N}._+\\-/]+"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()
    }

    private fun isHiddenPlaceholder(value: String): Boolean {
        return value in setOf(
            "content hidden",
            "contents hidden",
            "notification content hidden",
            "message content hidden",
            "hidden content"
        ) || value.contains("content hidden")
    }

    private fun isUnsupportedPlaceholder(value: String): Boolean {
        return value in setOf(
            "unsupported message",
            "message type unsupported",
            "this message type is not supported",
            "unsupported content"
        )
    }

    private fun isMissedCallPlaceholder(value: String): Boolean {
        return value in setOf(
            "missed call",
            "missed voice call",
            "missed video call",
            "missed whatsapp call"
        ) || value.contains("missed voice call") ||
            value.contains("missed video call") ||
            value.contains("missed call")
    }

    private fun isSystemNotificationText(value: String): Boolean {
        return value.contains("checking for new messages") ||
            value.contains("whatsapp web is currently active") ||
            value.contains("messages are syncing") ||
            value.contains("you may have new messages") ||
            value.contains("backup in progress") ||
            value.contains("backing up messages") ||
            value.contains("waiting for wi-fi")
    }

    private fun isPhotoPlaceholder(value: String): Boolean {
        return value in setOf(
            "photo",
            "image",
            "picture",
            "sent a photo",
            "sent an image",
            "sent a picture"
        )
    }

    private fun isVideoPlaceholder(value: String): Boolean {
        return value in setOf(
            "video",
            "gif",
            "sent a video",
            "sent a gif"
        )
    }

    private fun isStickerPlaceholder(value: String): Boolean {
        return value in setOf(
            "sticker",
            "sent a sticker"
        )
    }

    private fun isAudioPlaceholder(value: String): Boolean {
        return value in setOf(
            "audio",
            "voice message",
            "sent an audio",
            "sent a voice message"
        )
    }

    private fun isDocumentPlaceholder(value: String): Boolean {
        return value in setOf(
            "document",
            "file",
            "pdf",
            "sent a document",
            "sent a file"
        ) || value.matches(Regex(".+\\.(pdf|docx?|xlsx?|pptx?|zip|rar|txt)"))
    }
}
