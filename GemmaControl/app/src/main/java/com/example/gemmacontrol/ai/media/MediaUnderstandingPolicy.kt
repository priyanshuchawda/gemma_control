package com.example.gemmacontrol.ai.media

import com.example.gemmacontrol.notifications.WhatsAppContentKind
import com.example.gemmacontrol.notifications.promptBodyText
import com.example.gemmacontrol.notifications.spokenSummaryText

enum class MediaAccessState {
    NotificationPlaceholderOnly,
    LocalUriWithGrant,
    UserSelectedFile,
    Unsupported
}

enum class MediaUnderstandingModelPath {
    None,
    FutureVisionUnderstanding
}

data class MediaUnderstandingDecision(
    val accessState: MediaAccessState,
    val canInspectMediaBytes: Boolean,
    val hasUserGrantedMediaAccess: Boolean,
    val requiresUserConfirmationBeforeAnalysis: Boolean,
    val userFacingText: String,
    val promptText: String,
    val modelPath: MediaUnderstandingModelPath,
    val reason: String
)

object MediaUnderstandingPolicy {
    private val mediaPlaceholderKinds = setOf(
        WhatsAppContentKind.PHOTO,
        WhatsAppContentKind.VIDEO,
        WhatsAppContentKind.STICKER,
        WhatsAppContentKind.AUDIO,
        WhatsAppContentKind.DOCUMENT
    )

    fun fromNotificationContent(
        contentKind: WhatsAppContentKind,
        rawText: String? = null
    ): MediaUnderstandingDecision {
        return if (contentKind in mediaPlaceholderKinds) {
            MediaUnderstandingDecision(
                accessState = MediaAccessState.NotificationPlaceholderOnly,
                canInspectMediaBytes = false,
                hasUserGrantedMediaAccess = false,
                requiresUserConfirmationBeforeAnalysis = false,
                userFacingText = contentKind.spokenSummaryText(rawText),
                promptText = contentKind.promptBodyText(rawText),
                modelPath = MediaUnderstandingModelPath.None,
                reason = "WhatsApp notification content is a placeholder only; no media bytes or URI are available."
            )
        } else {
            MediaUnderstandingDecision(
                accessState = MediaAccessState.Unsupported,
                canInspectMediaBytes = false,
                hasUserGrantedMediaAccess = false,
                requiresUserConfirmationBeforeAnalysis = false,
                userFacingText = contentKind.spokenSummaryText(rawText),
                promptText = contentKind.promptBodyText(rawText),
                modelPath = MediaUnderstandingModelPath.None,
                reason = "This notification content kind is not an analyzable media input."
            )
        }
    }

    fun forUserSelectedFile(
        displayName: String,
        mimeType: String
    ): MediaUnderstandingDecision {
        return selectedMediaDecision(
            accessState = MediaAccessState.UserSelectedFile,
            displayName = displayName,
            mimeType = mimeType,
            reason = "The user selected this media item for this analysis flow."
        )
    }

    fun forLocalUriWithGrant(
        displayName: String,
        mimeType: String
    ): MediaUnderstandingDecision {
        return selectedMediaDecision(
            accessState = MediaAccessState.LocalUriWithGrant,
            displayName = displayName,
            mimeType = mimeType,
            reason = "The app has a persisted user-granted media URI for this analysis flow."
        )
    }

    private fun selectedMediaDecision(
        accessState: MediaAccessState,
        displayName: String,
        mimeType: String,
        reason: String
    ): MediaUnderstandingDecision {
        val safeLabel = displayName.ifBlank { "selected media" }
        val normalizedMimeType = mimeType.lowercase()
        val isSupportedVisualMedia = normalizedMimeType.startsWith("image/") ||
            normalizedMimeType.startsWith("video/")

        return if (isSupportedVisualMedia) {
            MediaUnderstandingDecision(
                accessState = accessState,
                canInspectMediaBytes = true,
                hasUserGrantedMediaAccess = true,
                requiresUserConfirmationBeforeAnalysis = true,
                userFacingText = "Selected media is available for analysis after confirmation.",
                promptText = "Actual user-selected media bytes are available for '$safeLabel' with MIME type '$mimeType'.",
                modelPath = MediaUnderstandingModelPath.FutureVisionUnderstanding,
                reason = reason
            )
        } else {
            MediaUnderstandingDecision(
                accessState = MediaAccessState.Unsupported,
                canInspectMediaBytes = false,
                hasUserGrantedMediaAccess = true,
                requiresUserConfirmationBeforeAnalysis = false,
                userFacingText = "This selected file type is not supported for media analysis yet.",
                promptText = "Selected file '$safeLabel' with MIME type '$mimeType' is unsupported for media understanding.",
                modelPath = MediaUnderstandingModelPath.None,
                reason = "Only user-selected image/video media is in the future analysis path."
            )
        }
    }
}
