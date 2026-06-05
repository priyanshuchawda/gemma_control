package com.example.gemmacontrol.notifications

fun WhatsAppContentKind.localReadSummaryText(rawText: String?): String {
    return when (this) {
        WhatsAppContentKind.TEXT -> rawText.nonBlankOr("[No message text]")
        WhatsAppContentKind.PHOTO -> "Photo attachment (contents not inspected)"
        WhatsAppContentKind.VIDEO -> "Video attachment (contents not inspected)"
        WhatsAppContentKind.STICKER -> "Sticker (contents not inspected)"
        WhatsAppContentKind.AUDIO -> "Audio or voice message (not transcribed)"
        WhatsAppContentKind.DOCUMENT -> "Document attachment (contents not inspected)"
        WhatsAppContentKind.MISSED_CALL -> "Missed WhatsApp call"
        WhatsAppContentKind.SYSTEM -> "WhatsApp system notification"
        WhatsAppContentKind.HIDDEN -> "Content hidden or unavailable"
        WhatsAppContentKind.UNKNOWN -> rawText.nonBlankOr("Unsupported WhatsApp content")
    }
}

fun WhatsAppContentKind.promptBodyText(rawText: String?): String {
    return when (this) {
        WhatsAppContentKind.TEXT -> rawText.nonBlankOr("[content unavailable]")
        WhatsAppContentKind.PHOTO -> "Photo attachment (contents not inspected)"
        WhatsAppContentKind.VIDEO -> "Video attachment (contents not inspected)"
        WhatsAppContentKind.STICKER -> "Sticker (contents not inspected)"
        WhatsAppContentKind.AUDIO -> "Audio or voice message (not transcribed)"
        WhatsAppContentKind.DOCUMENT -> "Document attachment (contents not inspected)"
        WhatsAppContentKind.MISSED_CALL -> "Missed WhatsApp call"
        WhatsAppContentKind.SYSTEM -> "WhatsApp system notification"
        WhatsAppContentKind.HIDDEN -> "Content hidden or unavailable"
        WhatsAppContentKind.UNKNOWN -> rawText.nonBlankOr("[unsupported WhatsApp content]")
    }
}

fun WhatsAppContentKind.spokenSummaryText(rawText: String?): String {
    return when (this) {
        WhatsAppContentKind.TEXT -> rawText.nonBlankOr("No message content")
        WhatsAppContentKind.PHOTO -> "Photo attachment. I cannot inspect image contents from the notification"
        WhatsAppContentKind.VIDEO -> "Video attachment. I cannot inspect video contents from the notification"
        WhatsAppContentKind.STICKER -> "Sticker. I cannot inspect sticker contents from the notification"
        WhatsAppContentKind.AUDIO -> "Audio or voice message. I cannot transcribe it from the notification"
        WhatsAppContentKind.DOCUMENT -> "Document attachment. I cannot inspect file contents from the notification"
        WhatsAppContentKind.MISSED_CALL -> "Missed WhatsApp call"
        WhatsAppContentKind.SYSTEM -> "WhatsApp system notification"
        WhatsAppContentKind.HIDDEN -> "Content hidden or unavailable"
        WhatsAppContentKind.UNKNOWN -> rawText.nonBlankOr("Unsupported WhatsApp content")
    }
}

fun WhatsAppContentKind.searchableText(rawText: String?): String {
    return listOfNotNull(
        name.lowercase(),
        localReadSummaryText(rawText),
        rawText?.takeIf { it.isNotBlank() }
    ).joinToString(separator = " ")
}

fun WhatsAppContentKind.isSystemNotification(): Boolean = this == WhatsAppContentKind.SYSTEM

private fun String?.nonBlankOr(fallback: String): String {
    return this?.takeIf { it.isNotBlank() } ?: fallback
}
