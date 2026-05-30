package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent

enum class HomeHeroAction {
    OpenVoiceAssistant,
    RequestNotificationAccess
}

data class HomeDashboardSummary(
    val heroAction: HomeHeroAction,
    val heroActionLabel: String,
    val statusTitle: String,
    val statusSubtitle: String,
    val capturedEventsCount: Int,
    val activeNotificationsCount: Int,
    val latestConversationTitle: String?,
    val emptyTitle: String,
    val emptySubtitle: String
)

fun buildHomeDashboardSummary(
    notifications: List<ParsedWhatsAppNotificationEvent>,
    isPermissionGranted: Boolean
): HomeDashboardSummary {
    val capturedEventsCount = notifications.size
    val activeNotificationsCount = notifications.count { it.isCurrentlyActive }
    val latestConversationTitle = notifications.firstNotNullOfOrNull { it.conversationTitle }
    val heroAction = if (isPermissionGranted) {
        HomeHeroAction.OpenVoiceAssistant
    } else {
        HomeHeroAction.RequestNotificationAccess
    }

    return HomeDashboardSummary(
        heroAction = heroAction,
        heroActionLabel = when (heroAction) {
            HomeHeroAction.OpenVoiceAssistant -> "Speak to GemmaControl"
            HomeHeroAction.RequestNotificationAccess -> "Grant notification access"
        },
        statusTitle = if (isPermissionGranted) {
            "Ready for voice actions"
        } else {
            "Setup needs attention"
        },
        statusSubtitle = when {
            !isPermissionGranted -> "Notification access is off, so WhatsApp context cannot refresh."
            activeNotificationsCount > 0 -> activeNotificationSubtitle(activeNotificationsCount)
            capturedEventsCount > 0 -> "Recent WhatsApp context is available for review."
            else -> "Keep GemmaControl open while new WhatsApp notifications arrive."
        },
        capturedEventsCount = capturedEventsCount,
        activeNotificationsCount = activeNotificationsCount,
        latestConversationTitle = latestConversationTitle,
        emptyTitle = "Waiting for WhatsApp",
        emptySubtitle = "Open WhatsApp or ask someone to send a message. New notifications appear here instantly."
    )
}

private fun activeNotificationSubtitle(count: Int): String {
    val noun = if (count == 1) "notification" else "notifications"
    return "$count active WhatsApp $noun ready for reply."
}
