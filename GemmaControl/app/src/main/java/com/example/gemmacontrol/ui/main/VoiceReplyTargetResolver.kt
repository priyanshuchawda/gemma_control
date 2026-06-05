package com.example.gemmacontrol.ui.main

data class ActiveReplyTarget(
    val notificationKey: String,
    val conversationTitle: String,
    val postedAt: Long
)

sealed interface VoiceReplyTargetResolution {
    data class Active(val draft: PendingVoiceReply) : VoiceReplyTargetResolution
    data class Draft(val conversationName: String, val replyText: String) : VoiceReplyTargetResolution
    data class Clarification(val prompt: String) : VoiceReplyTargetResolution
}

object VoiceReplyTargetResolver {
    fun resolveLatest(
        replyText: String,
        explicitLatest: Boolean,
        targets: List<ActiveReplyTarget>
    ): VoiceReplyTargetResolution {
        val sortedTargets = targets.sortedByDescending { it.postedAt }
        return when {
            sortedTargets.isEmpty() -> VoiceReplyTargetResolution.Clarification(
                "No active WhatsApp reply notification is available. To avoid sending to the wrong chat, say: draft reply to Mom: ${replyText.trim()}."
            )
            sortedTargets.size == 1 || explicitLatest -> sortedTargets.first().toActiveResolution(replyText)
            else -> VoiceReplyTargetResolution.Clarification(multipleTargetsPrompt(sortedTargets))
        }
    }

    fun resolveNamed(
        conversationName: String,
        replyText: String,
        targets: List<ActiveReplyTarget>
    ): VoiceReplyTargetResolution {
        val cleanedConversation = conversationName.trim()
        val matchingTarget = targets
            .sortedByDescending { it.postedAt }
            .firstOrNull { it.conversationTitle.equals(cleanedConversation, ignoreCase = true) }

        return matchingTarget?.toActiveResolution(replyText)
            ?: VoiceReplyTargetResolution.Draft(cleanedConversation, replyText.trim())
    }

    private fun ActiveReplyTarget.toActiveResolution(replyText: String): VoiceReplyTargetResolution.Active {
        return VoiceReplyTargetResolution.Active(
            PendingVoiceReply(
                notificationKey = notificationKey,
                replyText = replyText.trim(),
                conversationTitle = conversationTitle
            )
        )
    }

    private fun multipleTargetsPrompt(targets: List<ActiveReplyTarget>): String {
        val visibleTargets = targets.take(MaxTargetsInPrompt).joinToString { it.conversationTitle }
        val hiddenCount = targets.size - MaxTargetsInPrompt
        val suffix = if (hiddenCount > 0) {
            ", plus $hiddenCount more"
        } else {
            ""
        }
        return "Which chat should I reply to? Active reply targets: $visibleTargets$suffix."
    }

    private const val MaxTargetsInPrompt = 3
}
