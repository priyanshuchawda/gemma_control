package com.example.gemmacontrol.ui.main

import androidx.compose.ui.graphics.Color
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import java.text.SimpleDateFormat
import java.util.Date

internal data class NotificationBadgeColors(
    val background: Color,
    val foreground: Color
)

internal data class NotificationEventCardPresentation(
    val observedTime: String,
    val safeKey: String,
    val eventBadgeColors: NotificationBadgeColors,
    val conversationBadgeColors: NotificationBadgeColors,
    val activeLabel: String,
    val activeColor: Color
)

internal fun notificationEventCardPresentation(
    event: ParsedWhatsAppNotificationEvent,
    formatter: SimpleDateFormat
): NotificationEventCardPresentation {
    val activeColor = if (event.isCurrentlyActive) Green800 else Grey600
    return NotificationEventCardPresentation(
        observedTime = formatter.format(Date(event.observedAt)),
        safeKey = event.notificationKey.safeNotificationKey(),
        eventBadgeColors = event.eventType.badgeColors(),
        conversationBadgeColors = event.conversationType.badgeColors(),
        activeLabel = if (event.isCurrentlyActive) "Active" else "Expired",
        activeColor = activeColor
    )
}

private fun String.safeNotificationKey(): String =
    if (length > NotificationKeyVisibleSuffixLength) {
        "…${takeLast(NotificationKeyVisibleSuffixLength)}"
    } else {
        this
    }

private fun NotificationEventType.badgeColors(): NotificationBadgeColors =
    when (this) {
        NotificationEventType.POSTED -> NotificationBadgeColors(GreenBg, Green800)
        NotificationEventType.UPDATED -> NotificationBadgeColors(BlueBg, Blue800)
        NotificationEventType.REMOVED -> NotificationBadgeColors(RedBg, Red800)
    }

private fun ConversationType.badgeColors(): NotificationBadgeColors =
    when (this) {
        ConversationType.DIRECT -> NotificationBadgeColors(TealBg, Teal700)
        ConversationType.GROUP -> NotificationBadgeColors(PurpleBg, Purple700)
        ConversationType.UNKNOWN -> NotificationBadgeColors(OrangeBg, Orange800)
    }

private const val NotificationKeyVisibleSuffixLength = 8
